# CachePurge

An efficient, lightweight Xposed/LSPosed module that automatically clears application caches upon launch.

![Android](https://img.shields.io/badge/Android-11%2B-green.svg) ![LSPosed](https://img.shields.io/badge/LSPosed-Module-blue.svg) ![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)

## 📖 Overview

**CachePurge** is designed to keep your device clean and maintain storage space automatically. Instead of manually clearing caches for bloated apps, this module hooks into the application startup process and silently purges temporary files in the background.

It is built with a **"Steel Core" architecture**, ensuring zero impact on app startup time (Cold Start) and strict safety protocols to prevent accidental data loss.

## ✨ Features

* **🚀 Automated Cleanup:** Triggers automatically when the target application starts (`Application.onCreate`).
* **🧹 Deep Cleaning:** Removes files from:
    * Internal Cache (`/data/user/0/.../cache`)
    * External Cache (`/sdcard/Android/data/.../cache`)
    * Code Cache (`code_cache` - JIT compiled artifacts)
* **⚡ Zero-Lag Performance:**
    * Runs on a dedicated **Background Thread** with `MIN_PRIORITY`.
    * Does not block the UI thread or slow down app launch.
* **🛡️ Safety First:**
    * **Scope Isolation:** Only runs on apps you explicitly select in LSPosed.
    * **Smart Deletion:** Recursively cleans files while preserving directory structures to prevent permission errors.
    * **Fail-Safe:** Built-in safeguards to prevent hooking into critical system processes (e.g., `android`, `com.android.systemui`).

## 📱 Requirements

* **[span_0](start_span)Android OS:** Android 11 (SDK 30) or higher[span_0](end_span).
* **Root Access:** Magisk or KernelSU.
* **Xposed Framework:** LSPosed (Zygisk or Riru variant recommended).

## 🛠️ Installation & Usage

1.  **Install:** Download and install the `app-release.apk`.
2.  **Activate:** Open the **LSPosed Manager** notification.
3.  **Enable:** Toggle the switch to enable **CachePurge**.
4.  **Scope (Crucial Step):**
    * Select the applications you want to auto-clean (e.g., Facebook, Instagram, TikTok, Chrome).
    * **⚠️ WARNING:** Do NOT scope "System Framework" (android) or "System UI" unless you are a developer debugging specific issues. The module has built-in protections, but it is best practice to only scope user apps.
5.  **Reboot:** Restart your device (or force stop the target apps) for changes to take effect.

## 🔍 How It Works

Technically, CachePurge hooks the `onCreate` method of the `android.app.Application` class.
1.  It verifies the package name to ensure it matches the target scope.
2.  It spawns a low-priority daemon thread.
3.  It safely iterates through standard Android cache directories (`context.cacheDir`, `context.externalCacheDir`, `context.codeCacheDir`) and deletes the contents.

## 📝 Logs & Debugging

If you need to check if the module is working or report an issue, you can view the logs via ADB or Termux (Root):

```bash
# Filter specifically for CachePurge logs
su -c "logcat -s CachePurge"
```

⚠️ Disclaimer
This module deletes data. While it targets temporary cache files only (which are safe to delete), the developer is not responsible for any unintended data loss or app instability. Use at your own risk.
📄 License
This project is open source.
Built with ❤️ by [thoitiettxl-cyber]