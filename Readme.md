# Custom Instructions for Cpatcher: Advanced Android Runtime Manipulation Framework

## Strategic Framework Overview

**Project Classification**: System-level Android runtime modification framework leveraging LSPosed/Xposed architecture with unrestricted hidden API access via Rikka's bypass mechanism.

**Technical Capabilities**: Direct manipulation of Android internals through compile-time API stub integration, enabling surgical precision in system behavior modification without reflection overhead.

## Stage A: Technical Architecture Specifications

### 1. Core Technology Stack Analysis

**Framework Components**:
```kotlin
// Primary Dependencies Configuration
dependencies {
    compileOnly(libs.xposed.api)           // v82 - Hook infrastructure
    compileOnly(libs.dev.rikka.hidden.stub) // Hidden API compile stubs
    implementation(libs.dev.rikka.hidden.compat) // Runtime compatibility
    implementation(libs.dexkit)            // v2.0.6 - Obfuscation analysis
}

// Plugin Architecture
plugins {
    alias(libs.plugins.refine)  // Hidden API bytecode manipulation
}
```

**Operational Parameters**:
- Target SDK Range: 26-36 (Android 8.0 - Android 14+)
- Architecture Constraint: arm64-v8a exclusive
- Kotlin Version: 2.1.21 with JVM 17 target
- Module Type: LSPosed/EdXposed compatible

### 2. Project Structure Blueprint

```
io.github.cpatcher/
├── Entry.kt                    # Module initialization vector
├── log.kt                      # Centralized logging infrastructure
├── arch/                       # Architecture utilities
│   ├── IHook.kt               # Base hook abstraction
│   ├── HookUtils.kt           # Extension-based hook framework
│   ├── ObfsUtils.kt           # DexKit obfuscation bypass
│   ├── BacktraceUtils.kt      # Stack trace analysis
│   ├── ExtraField.kt          # Dynamic field injection
│   ├── PrefUtils.kt           # Preference management
│   ├── ReflectUtil.kt         # Reflection utilities
│   └── Utils.kt               # General utilities
├── bridge/                     # Xposed API wrapper layer
│   ├── Xposed.java           # Static method bridge
│   ├── HookParam.java        # Parameter encapsulation
│   ├── LoadPackageParam.java # Package context
│   ├── MethodHookCallback.java # Callback abstraction
│   └── Unhook.kt             # Hook management
└── handlers/                   # Feature implementations
    └── UniversalCachePurgeHandler.kt
```

## Stage B: Implementation Patterns & Conventions

### 3. Advanced Hook Implementation Pattern

```kotlin
class SystemLevelHandler : IHook() {
    companion object {
        // Configuration constants - UPPER_SNAKE_CASE
        private const val ENABLE_HIDDEN_API = true
        private const val ASYNC_EXECUTION = true
    }
    
    override fun onHook() {
        // Stage 1: Package isolation verification
        val targetPackage = loadPackageParam.packageName
        val targetProcess = loadPackageParam.processName
        
        // Critical: Process filtering
        if (targetProcess != targetPackage) {
            logI("${this::class.simpleName}: Process exclusion: $targetProcess")
            return
        }
        
        // Stage 2: Hidden API initialization
        initializeHiddenAPIs()
        
        // Stage 3: Hook deployment
        deploySystemHooks()
    }
    
    private fun initializeHiddenAPIs() {
        // Direct hidden API access without reflection
        val activityThread = android.app.ActivityThread.currentActivityThread()
        val instrumentation = activityThread.mInstrumentation
        val packageManager = android.content.pm.IPackageManager.Stub.asInterface(
            android.os.ServiceManager.getService("package")
        )
    }
    
    private fun deploySystemHooks() {
        // Multi-vector interception strategy
        android.app.Activity::class.java.hookAllAfter("onCreate") { param ->
            performStrategicIntervention(param)
        }
    }
}
```

### 4. Extension Function Architecture

**Inline Hook Extensions** (Performance-Critical):
```kotlin
// Primary hook pattern with condition evaluation
inline fun Class<*>.hookAllAfter(
    name: String,
    crossinline cond: () -> Boolean = { true },
    crossinline fn: HookCallback
) = Xposed.hookAllMethods(this, name, object : MethodHookCallback() {
    override fun afterHook(param: HookParam) {
        if (cond()) fn(param)
    }
})

// Replacement pattern for method interception
inline fun Method.hookReplace(
    crossinline cond: () -> Boolean = { true },
    crossinline replacement: HookReplacement
): Unhook = Xposed.hookMethod(this, object : MethodHookCallback() {
    override fun beforeHook(param: HookParam) {
        if (cond()) {
            try {
                param.result = replacement(param)
            } catch (t: Throwable) {
                param.throwable = t
            }
        }
    }
})
```

### 5. Obfuscation Bypass Framework

```kotlin
// DexKit integration for obfuscated target analysis
fun IHook.createObfsTable(
    name: String,
    tableVersion: Int,
    classLoader: ClassLoader? = null,
    creator: (DexKitBridge) -> ObfsTable
): ObfsTable {
    val appInfo = loadPackageParam.appInfo
    val apkPath = appInfo.sourceDir
    
    // Cache-based optimization
    val tableFile = File(appInfo.dataDir, "cache/obfs_table_$name.json")
    
    // Validation and regeneration logic
    return validateAndLoadCache(tableFile, tableVersion) ?: run {
        _loadDexKit  // Native library initialization
        val bridge = DexKitBridge.create(classLoader ?: apkPath)
        creator(bridge).also { saveObfsTable(tableFile, it, tableVersion) }
    }
}
```

## Stage C: Code Generation Directives

### 6. Naming Convention Enforcement

**Class Naming**:
- Handlers: `*Handler` suffix (e.g., `UniversalCachePurgeHandler`)
- Utilities: `*Utils` suffix (e.g., `HookUtils`, `ObfsUtils`)  
- Bridges: Direct descriptive names (e.g., `HookParam`)
- Hooks: `*Hook` suffix for complex hooks

**Method Naming**:
- Hook methods: `hook*` prefix variations
  * `hookAll*` - Multiple method hooks
  * `hook*After/Before` - Timing specific
  * `hookReplace` - Full replacement
- Implementation methods: `implement*` prefix
- Execution methods: `execute*` or `perform*` prefix

**Constants**:
```kotlin
companion object {
    private const val PURGE_INTERNAL_CACHE = true  // Configuration flag
    const val OBFS_KEY_APK = "apkPath"            // Public key constant
    const val LOG_TAG = "CachePurge"              // Module identifier
}
```

### 7. Error Handling Protocol

```kotlin
// Standard error handling pattern
runCatching {
    // High-risk operation with hidden API access
    val systemService = android.os.ServiceManager.getService("activity")
    val activityManager = android.app.IActivityManager.Stub.asInterface(systemService)
    
    // Operation execution
    activityManager.forceStopPackage(packageName, userId)
    
}.onFailure { throwable ->
    logE("Operation context: Force stop failed for $packageName", throwable)
    // Fallback mechanism
    executeAlternativeStrategy()
}.onSuccess { result ->
    logI("Operation successful: $result")
}
```

### 8. Hidden API Access Patterns

**Direct System Service Access**:
```kotlin
// Traditional approach (AVOID)
val serviceManager = Class.forName("android.os.ServiceManager")
val method = serviceManager.getDeclaredMethod("getService", String::class.java)
val binder = method.invoke(null, "activity") as IBinder

// Rikka Hidden API approach (PREFERRED)
val binder = android.os.ServiceManager.getService("activity")
val activityManager = android.app.IActivityManager.Stub.asInterface(binder)
```

**Hidden Field Access**:
```kotlin
// Direct access to hidden fields
class ActivityAnalyzer {
    fun analyzeActivity(activity: Activity) {
        // Hidden field access without reflection
        val token = activity.mToken  // IBinder
        val activityInfo = activity.mActivityInfo  // ActivityInfo
        val instrumentation = activity.mInstrumentation  // Instrumentation
        
        // Strategic manipulation
        modifyActivityBehavior(token, activityInfo)
    }
}
```

## Stage D: Operational Guidelines

### 9. Package Isolation Protocol

```kotlin
// Mandatory isolation check pattern
override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
    // System package exclusion
    if (lpparam.packageName == "android" || 
        lpparam.packageName.startsWith("com.android.systemui")) {
        return  // Critical: Prevent system interference
    }
    
    // Process verification
    if (lpparam.processName != lpparam.packageName) {
        logI("Sub-process exclusion: ${lpparam.processName}")
        return
    }
    
    // Handler deployment
    deployHandlers(LoadPackageParam(lpparam))
}
```

### 10. Asynchronous Execution Strategy

```kotlin
private fun executeAsyncOperation(context: Context, operation: String) {
    if (ASYNC_EXECUTION) {
        thread(
            name = "CachePurge-${context.packageName}-$operation",
            priority = Thread.MIN_PRIORITY,
            start = true
        ) {
            performOperation(context)
        }
    } else {
        // Synchronous fallback
        performOperation(context)
    }
}
```

## Stage E: Advanced Techniques

### 11. Dynamic Field Injection

```kotlin
// ExtraField pattern for runtime data attachment
class HookState {
    var processingCount by extraField(this, "processing_count", 0)
    var lastExecutionTime by extraField(this, "last_exec", 0L)
    
    fun trackExecution() {
        processingCount++
        lastExecutionTime = System.currentTimeMillis()
    }
}
```

### 12. Stack Trace Analysis

```kotlin
// Advanced debugging with stack trace extraction
fun analyzeCallOrigin(): StackTraceElementEx {
    val throwable = Throwable()
    val stackTrace = getStackTraceEx(throwable)
    
    return stackTrace.firstOrNull { element ->
        element.dexLocation?.contains("target.apk") == true
    } ?: StackTraceElementEx()
}
```

## Quick Reference Matrix

### Critical Patterns
| Operation | Implementation | Priority |
|-----------|---------------|----------|
| Package Check | `if (processName != packageName) return` | MANDATORY |
| Hidden API | `android.app.ActivityThread.currentActivityThread()` | PREFERRED |
| Hook After | `Class.hookAllAfter("method") { }` | STANDARD |
| Error Handle | `runCatching { }.onFailure { logE() }` | REQUIRED |
| Async Exec | `thread(start = true) { }` | CONDITIONAL |

### Anti-Patterns to Avoid
- **NEVER** use raw reflection for hidden APIs
- **NEVER** skip package isolation verification
- **NEVER** catch without logging
- **AVOID** synchronous I/O in hooks
- **AVOID** hardcoded system package names

## Technical Rationale

The architectural decisions embedded in this framework represent optimal trade-offs between:

1. **Performance**: Direct hidden API invocation eliminates reflection overhead
2. **Resilience**: DexKit integration handles aggressive obfuscation
3. **Maintainability**: Extension functions provide clean, reusable patterns
4. **Compatibility**: Rikka framework ensures cross-version stability

This comprehensive approach ensures maximum operational flexibility while maintaining minimal detection surface and optimal runtime performance.
