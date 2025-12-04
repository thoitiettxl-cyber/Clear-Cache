package io.github.cpatcher.handlers

import android.app.Application
import android.content.Context
import io.github.cpatcher.arch.IHook
import io.github.cpatcher.arch.hookAllAfter
import io.github.cpatcher.logE
import io.github.cpatcher.logI
import io.github.cpatcher.logW
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * CleanHandler - Automatic cache cleanup module for LSPosed-scoped applications
 * 
 * Technical approach:
 * - Hooks into Application.onCreate() for earliest possible cleanup
 * - Performs asynchronous deletion to avoid blocking UI thread
 * - Implements comprehensive error containment per directory
 * - Tracks cleanup metrics for performance monitoring
 */
class CleanHandler : IHook() {
    companion object {
        private const val CLEANUP_THREAD_NAME = "CpatcherCacheCleanup"
        private const val CLEANUP_TIMEOUT_MS = 5000L
        private val cleanupExecutor = Executors.newSingleThreadExecutor { r ->
            Thread(r, CLEANUP_THREAD_NAME).apply {
                priority = Thread.MIN_PRIORITY
                isDaemon = true
            }
        }
    }
    
    override fun onHook() {
        // MANDATORY: Package validation - only process if we're in a targeted app
        // Since this is a universal handler, we check if it's NOT our own module
        if (loadPackageParam.packageName == "io.github.cpatcher") {
            logI("${this::class.simpleName}: Skipping self-cleanup")
            return
        }
        
        // Hook Application onCreate for earliest cleanup opportunity
        Application::class.java.hookAllAfter("onCreate") { param ->
            val application = param.thisObject as? Application
            if (application == null) {
                logW("${this::class.simpleName}: Unable to cast to Application")
                return@hookAllAfter
            }
            
            runCatching {
                performCacheCleanup(application)
            }.onFailure { t ->
                logE("${this::class.simpleName}: Failed to initiate cache cleanup", t)
            }
        }
        
        logI("${this::class.simpleName}: Successfully initialized for ${loadPackageParam.packageName}")
    }
    
    private fun performCacheCleanup(context: Context) {
        cleanupExecutor.execute {
            runCatching {
                val startTime = System.currentTimeMillis()
                var totalDeleted = 0L
                var filesDeleted = 0
                
                // External cache cleanup - typically largest
                context.externalCacheDir?.let { dir ->
                    if (dir.exists()) {
                        val result = deleteDirectoryContents(dir)
                        totalDeleted += result.first
                        filesDeleted += result.second
                        logI("${this::class.simpleName}: External cache - " +
                            "${result.second} files, ${result.first / 1024}KB")
                    }
                }
                
                // Internal cache cleanup
                context.cacheDir?.let { dir ->
                    if (dir.exists()) {
                        val result = deleteDirectoryContents(dir)
                        totalDeleted += result.first
                        filesDeleted += result.second
                        logI("${this::class.simpleName}: Internal cache - " +
                            "${result.second} files, ${result.first / 1024}KB")
                    }
                }
                
                // Code cache cleanup - JIT compiled code
                context.codeCacheDir?.let { dir ->
                    if (dir.exists()) {
                        val result = deleteDirectoryContents(dir)
                        totalDeleted += result.first
                        filesDeleted += result.second
                        logI("${this::class.simpleName}: Code cache - " +
                            "${result.second} files, ${result.first / 1024}KB")
                    }
                }
                
                val elapsed = System.currentTimeMillis() - startTime
                
                if (filesDeleted > 0) {
                    logI("${this::class.simpleName}: Cleanup completed - " +
                        "$filesDeleted files, ${totalDeleted / 1024}KB in ${elapsed}ms")
                } else {
                    logI("${this::class.simpleName}: No cache files to clean")
                }
                
            }.onFailure { t ->
                logE("${this::class.simpleName}: Cache cleanup thread failed", t)
            }
        }
    }
    
    /**
     * Recursively deletes directory contents while preserving the root directory
     * @return Pair of (total bytes deleted, file count)
     */
    private fun deleteDirectoryContents(directory: File): Pair<Long, Int> {
        var totalSize = 0L
        var fileCount = 0
        
        runCatching {
            directory.listFiles()?.forEach { file ->
                val result = deleteRecursively(file)
                totalSize += result.first
                fileCount += result.second
            }
        }.onFailure { t ->
            logE("${this::class.simpleName}: Failed to list ${directory.absolutePath}", t)
        }
        
        return Pair(totalSize, fileCount)
    }
    
    /**
     * Recursively deletes a file or directory
     * @return Pair of (total bytes deleted, file count)
     */
    private fun deleteRecursively(file: File): Pair<Long, Int> {
        if (!file.exists()) return Pair(0L, 0)
        
        var totalSize = 0L
        var fileCount = 0
        
        runCatching {
            if (file.isDirectory) {
                file.listFiles()?.forEach { child ->
                    val result = deleteRecursively(child)
                    totalSize += result.first
                    fileCount += result.second
                }
            }
            
            val fileSize = file.length()
            if (file.delete()) {
                totalSize += fileSize
                fileCount++
            }
            
        }.onFailure { t ->
            // Silent failure for individual files - don't spam logs
            if (file.isDirectory) {
                logW("${this::class.simpleName}: Failed to delete directory ${file.name}")
            }
        }
        
        return Pair(totalSize, fileCount)
    }
    
    /**
     * Cleanup executor on module unload (if supported by framework)
     */
    fun cleanup() {
        runCatching {
            cleanupExecutor.shutdown()
            if (!cleanupExecutor.awaitTermination(CLEANUP_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                cleanupExecutor.shutdownNow()
            }
        }.onFailure { t ->
            logE("${this::class.simpleName}: Failed to shutdown executor", t)
        }
    }
}