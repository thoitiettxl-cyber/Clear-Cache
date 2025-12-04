package io.github.cpatcher.handlers

import android.app.Application
import android.app.ActivityThread // Hidden API (Rikka Stub)
import android.os.Process
import android.os.UserHandle // Hidden API (Rikka Stub)
import io.github.cpatcher.arch.IHook
import io.github.cpatcher.arch.hookAfter
import io.github.cpatcher.logI
import io.github.cpatcher.logE
import io.github.cpatcher.logW
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

/**
 * CleanHandler (Hybrid Version)
 * * Kết hợp sức mạnh của:
 * 1. HookUtils DSL: Để can thiệp vào vòng đời Application.
 * 2. Hidden APIs: Để truy xuất thông tin hệ thống (Process, User) không qua Reflection.
 */
class CleanHandler : IHook() {

    companion object {
        private const val THREAD_NAME = "Cpatcher-Cleaner"
        
        // Tối ưu hóa Thread: Priority thấp để không ảnh hưởng khởi động App
        private val cleanupExecutor = Executors.newSingleThreadExecutor { r ->
            Thread(r, THREAD_NAME).apply {
                priority = Thread.MIN_PRIORITY
                isDaemon = true
            }
        }
    }

    override fun onHook() {
        // RULE 3: Isolation - Logic nằm gọn trong IHook
        if (loadPackageParam.packageName == "io.github.cpatcher") return

        // RULE 2: HookUtils DSL Standard
        // Thay vì XposedHelpers.findAndHookMethod, ta dùng extension function
        Application::class.java.hookAfter("onCreate") { param ->
            val app = param.thisObject as? Application ?: return@hookAfter

            runCatching {
                // RULE 1: Hybrid Interaction Mandate
                // Gọi trực tiếp Hidden API, KHÔNG dùng Reflection.
                // ActivityThread.currentProcessName() và UserHandle.myUserId()
                // được cung cấp bởi thư viện dev.rikka.hidden.
                val currentProcess = ActivityThread.currentProcessName()
                val userId = UserHandle.myUserId()

                logI("Initiating cache purge for $currentProcess (UID: ${Process.myUid()}, User: $userId)")
                
                performHybridCleanup(app)
            }.onFailure {
                logE("Failed to init cleanup hook", it)
            }
        }
    }

    private fun performHybridCleanup(context: Application) {
        cleanupExecutor.execute {
            runCatching {
                val start = System.currentTimeMillis()
                var freedBytes = 0L
                var fileCount = 0

                // Danh sách các thư mục cache tiêu chuẩn
                val cacheDirs = listOfNotNull(
                    context.externalCacheDir,
                    context.cacheDir,
                    context.codeCacheDir
                )

                cacheDirs.forEach { dir ->
                    if (dir.exists()) {
                        val (bytes, count) = deleteRecursively(dir)
                        freedBytes += bytes
                        fileCount += count
                    }
                }

                if (fileCount > 0) {
                    val time = System.currentTimeMillis() - start
                    logI("Cleanup complete: Removed $fileCount files (${freedBytes / 1024} KB) in ${time}ms")
                }
            }.onFailure {
                logE("Cleanup thread error", it)
            }
        }
    }

    // Helper: Đệ quy xóa file an toàn, trả về (Bytes, Số lượng)
    private fun deleteRecursively(file: File): Pair<Long, Int> {
        var size = 0L
        var count = 0
        
        if (!file.exists()) return 0L to 0

        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                val (childSize, childCount) = deleteRecursively(child)
                size += childSize
                count += childCount
            }
        }

        // Không xóa thư mục gốc (cache, code_cache) để tránh lỗi permission cục bộ
        // Chỉ xóa nội dung bên trong hoặc thư mục con
        val isRootCache = file.name == "cache" || file.name == "code_cache"
        if (!isRootCache) {
            val length = file.length()
            if (file.delete()) {
                size += length
                count++
            }
        }
        
        return size to count
    }
}
