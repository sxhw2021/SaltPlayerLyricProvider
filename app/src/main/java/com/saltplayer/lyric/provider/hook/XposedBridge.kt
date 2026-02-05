package com.saltplayer.lyric.provider.hook

import java.lang.reflect.Method
import java.lang.reflect.Constructor

object XposedBridge {
    private var xposedHelpersClass: Class<*>? = null
    private var xposedBridgeClass: Class<*>? = null
    private var methodHookClass: Class<*>? = null

    private var findClassMethod: Method? = null
    private var findMethodMethod: Method? = null
    private var findConstructorMethod: Method? = null
    private var hookMethodMethod: Method? = null
    private var invokeOriginalMethod: Method? = null

    private var beforeHookedMethodField: java.lang.reflect.Field? = null
    private var afterHookedMethodField: java.lang.reflect.Field? = null

    private var isInitialized = false

    fun initialize(classLoader: ClassLoader): Boolean {
        if (isInitialized) return true

        try {
            xposedHelpersClass = classLoader.loadClass("de.robv.android.xposed.XposedHelpers")
            xposedBridgeClass = classLoader.loadClass("de.robv.android.xposed.XposedBridge")
            methodHookClass = classLoader.loadClass("de.robv.android.xposed.callbacks.XC_MethodHook")

            findClassMethod = xposedHelpersClass?.getMethod(
                "findClassIfExists",
                String::class.java,
                ClassLoader::class.java
            )

            findMethodMethod = xposedHelpersClass?.getMethod(
                "findMethodExactIfExists",
                Class::class.java,
                String::class.java,
                Array<Class<*>>::class.java
            )

            findConstructorMethod = xposedHelpersClass?.getMethod(
                "findConstructorExactIfExists",
                Class::class.java,
                Array<Class<*>>::class.java
            )

            hookMethodMethod = xposedHelpersClass?.getMethod(
                "findAndHookMethod",
                Any::class.java,
                String::class.java,
                Array<Any>::class.java,
                methodHookClass
            )

            invokeOriginalMethod = xposedBridgeClass?.getMethod(
                "invokeOriginalMethod",
                Method::class.java,
                Any::class.java,
                Array<Any>::class.java
            )

            beforeHookedMethodField = methodHookClass?.getDeclaredField("beforeHookedMethod")
            beforeHookedMethodField?.isAccessible = true

            afterHookedMethodField = methodHookClass?.getDeclaredField("afterHookedMethod")
            afterHookedMethodField?.isAccessible = true

            isInitialized = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun findClass(className: String, classLoader: ClassLoader): Class<*>? {
        return try {
            findClassMethod?.invoke(null, className, classLoader) as? Class<*>
        } catch (e: Exception) {
            null
        }
    }

    fun findMethodExactIfExists(
        clazz: Class<*>,
        methodName: String,
        vararg parameterTypes: Class<*>
    ): Method? {
        return try {
            findMethodMethod?.invoke(
                null,
                clazz,
                methodName,
                parameterTypes
            ) as? Method
        } catch (e: Exception) {
            null
        }
    }

    fun findConstructorExactIfExists(
        clazz: Class<*>,
        vararg parameterTypes: Class<*>
    ): Constructor<*>? {
        return try {
            findConstructorMethod?.invoke(
                null,
                clazz,
                parameterTypes
            ) as? Constructor<*>
        } catch (e: Exception) {
            null
        }
    }

    fun hookMethod(
        clazz: Any,
        methodName: String,
        parameterTypes: Array<Class<*>>,
        callback: MethodHookCallback
    ) {
        try {
            val hookerClass = methodHookClass?.classLoader?.loadClass("de.robv.android.xposed.XposedHelpers\$MethodHooker")
            val hookerConstructor = hookerClass?.getDeclaredConstructor(methodHookClass)
            val hooker = hookerConstructor?.newInstance(callback)

            hookMethodMethod?.invoke(null, clazz, methodName, parameterTypes, hooker)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hookConstructor(
        clazz: Class<*>,
        parameterTypes: Array<Class<*>>,
        callback: MethodHookCallback
    ) {
        try {
            val hookerClass = methodHookClass?.classLoader?.loadClass("de.robv.android.xposed.XposedHelpers\$MethodHooker")
            val hookerConstructor = hookerClass?.getDeclaredConstructor(methodHookClass)
            val hooker = hookerConstructor?.newInstance(callback)

            val hookConstructorMethod = xposedHelpersClass?.getMethod(
                "findAndHookConstructor",
                Class::class.java,
                Array<Class<*>>::class.java,
                methodHookClass
            )

            hookConstructorMethod?.invoke(null, clazz, parameterTypes, hooker)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    abstract class MethodHookCallback {
        @Throws(Exception::class)
        abstract fun beforeHookedMethod(param: MethodHookParam)

        @Throws(Exception::class)
        abstract fun afterHookedMethod(param: MethodHookParam)
    }

    class MethodHookParam(
        val thisObject: Any?,
        val args: Array<Any?>,
        private val methodField: java.lang.reflect.Field?,
        private val hooker: Any?
    ) {
        private var resultValue: Any? = null
        private var thrown: Throwable? = null
        var result: Any?
            get() = try {
                resultField?.get(hooker) ?: resultValue
            } catch (e: Exception) {
                resultValue
            }
            set(value) {
                resultValue = value
                try {
                    resultField?.set(hooker, value)
                } catch (e: Exception) {
                }
            }

        var throwable: Throwable?
            get() = thrown
            set(value) {
                thrown = value
                try {
                    throwableField?.set(hooker, value)
                } catch (e: Exception) {
                }
            }

        private val resultField: java.lang.reflect.Field?
            get() = try {
                val resultFieldClass = hooker?.javaClass?.declaredField("\$result")
                resultFieldClass?.isAccessible = true
                resultFieldClass
            } catch (e: Exception) {
                null
            }

        private val throwableField: java.lang.reflect.Field?
            get() = try {
                val throwableFieldClass = hooker?.javaClass?.declaredField("\$throwable")
                throwableFieldClass?.isAccessible = true
                throwableFieldClass
            } catch (e: Exception) {
                null
            }
    }
}
