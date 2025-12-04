package io.github.cpatcher

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.cpatcher.bridge.LoadPackageParam
import io.github.cpatcher.handlers.CleanHandler

/**
 * Entry Point (Steel Core Version)
 * Cập nhật để đồng bộ với bộ quy tắc bảo mật và hiệu năng.
 */
class Entry : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Rule 5: Logging Convention
        // Thiết lập prefix ngay lập tức để log đồng nhất
        logPrefix = "[CachePurge] "

        // Rule 1: Scope Isolation (The Golden Rule) - Fail Fast Strategy
        // Kiểm tra và return NGAY LẬP TỨC nếu là các package bị cấm.
        // Điều này ngăn chặn việc cấp phát bộ nhớ cho CleanHandler không cần thiết.
        val pkg = lpparam.packageName
        if (pkg == "android" || 
            pkg.startsWith("com.android.systemui") || 
            pkg == "io.github.cpatcher") {
            return
        }

        // Rule 4: Resiliency - Zero Crash Policy
        // Bọc toàn bộ logic khởi tạo module để bảo vệ process của ứng dụng host
        runCatching {
            // Chỉ log khi chắc chắn sẽ inject (giảm spam logcat)
            logI("Entry: Injecting into $pkg (${lpparam.processName})")

            // Khởi tạo Bridge Param
            val param = LoadPackageParam(lpparam)
            
            // Kích hoạt Handler
            CleanHandler().hook(param)
            
        }.onFailure {
            // Rule 5: Log lỗi nghiêm trọng nếu entry point thất bại
            logE("Critical: Failed to initialize module entry", it)
        }
    }
}
