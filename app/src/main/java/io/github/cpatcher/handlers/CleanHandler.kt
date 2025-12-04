package io.github.cpatcher.handlers

import android.app.Application
import android.os.Process
import io.github.cpatcher.arch.IHook
import io.github.cpatcher.arch.hookAfter
import io.github.cpatcher.logI
import io.github.cpatcher.logE
import java.io.File
import java.util.concurrent.Executors

/**
 * CleanHandler (Fixed Version)
 * Sử dụng thông tin có sẵn từ IHook (như mã gốc) để tránh lỗi thiếu API ẩn.
 */
class CleanHandler : IHook() {

    companion object {
        private const val THREAD_NAME = "Cpatcher-Cleaner"
        
        // Priority thấp để không làm chậm App khi khởi động
        private val cleanupExecutor = Executors.newSingleThreadExecutor { r ->
            Thread(r, THREAD_NAME).apply {
                priority = Thread.MIN_PRIORITY
                isDaemon = true
            }
        }
    }

    override fun onHook() {
        // 1. Dùng loadPackageParam có sẵn (Giống mã gốc) - Không cần ActivityThread
        // loadPackageParam được thừa hưởng từ lớp cha IHook
        if (loadPackageParam.packageName == "io.github.cpatcher") return

        // 2. Hook bằng DSL (Cải tiến mới)
        Application::class.java.hookAfter("onCreate") { param ->
            val app = param.thisObject as? Application ?: return@hookAfter

            runCatching {
                // Lấy thông tin an toàn
                val pkgName = loadPackageParam.packageName
                val procName = loadPackageParam.processName
                val uid = Process.myUid() // API chuẩn Android, không cần Stub

                logI("CachePurge Init: $pkgName ($procName) [UID: $uid]")
                
                performCleanup(app)
            }.onFailure {
                logE("Failed to init cleanup", it)
            }
        }
    }

    private fun performCleanup(context: Application) {
        cleanupExecutor.execute {
            runCatching {
                val start = System.currentTimeMillis()
                var freedBytes = 0L
                var fileCount = 0

                // Context tự động trỏ đến đúng thư mục của User hiện tại (Giống mã gốc)
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
                    logI("Cleaned: $fileCount files (${freedBytes / 1024} KB) in ${time}ms")
                }
            }.onFailure {
                // Silent fail để không spam log nếu không có quyền
            }
        }
    }

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

        // Không xóa thư mục gốc, chỉ xóa nội dung
        val isRoot = file.name == "cache" || file.name == "code_cache"
        if (!isRoot) {
            val length = file.length()
            if (file.delete()) {
                size += length
                count++
            }
        }
        
        return size to count
    }
}
