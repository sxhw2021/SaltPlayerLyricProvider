package com.saltplayer.lyric.provider.hook

import java.lang.reflect.Method

object XposedBridge {
    private var xposedHelpersClass: Class<*>? = null
    private var xposedBridgeClass: Class<*>? = null
    private var methodHookClass: Class<*>? = null

    private var findClassMethod: Method? = null
    private var findMethodMethod: Method? = null
    private var findConstructorMethod: Method? = null
    private var hookMethodMethod: Method? = null
    private var hookConstructorMethod: Method? = null

    private var isInitialized = false

    @Suppress("UNCHECKED_CAST")
    private fun <T> Array<T>.toClassArray(): Array<Class<T>> {
        return Array(this.size) { this[it]::class.java }
    }

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

            val paramTypes1 = arrayOf<Class<*>>(
                Class::class.java,
                String::class.java,
                Array<Class<*>>::class.java
            )
            findMethodMethod = xposedHelpersClass?.getMethod(
                "findMethodExactIfExists",
                *paramTypes1
            )

            val paramTypes2 = arrayOf<Class<*>>(
                Class::class.java,
                Array<Class<*>>::class.java
            )
            findConstructorMethod = xposedHelpersClass?.getMethod(
                "findConstructorExactIfExists",
                *paramTypes2
            )

            val paramTypes3 = arrayOf<Class<*>>(
                Class::class.java,
                String::class.java,
                Array<Any>::class.java,
                methodHookClass
            )
            hookMethodMethod = xposedHelpersClass?.getMethod(
                "findAndHookMethod",
                *paramTypes3
            )

            val paramTypes4 = arrayOf<Class<*>>(
                Class::class.java,
                Array<Any>::class.java,
                methodHookClass
            )
            hookConstructorMethod = xposedHelpersClass?.getMethod(
                "findAndHookConstructor",
                *paramTypes4
            )

            isInitialized = true
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun findClass(className: String, classLoader: ClassLoader): Class<*>? {
        return try {
            findClassMethod?.invoke(null, className, classLoader) as? Class<*>
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun findMethodExact(
        clazz: Class<*>,
        methodName: String,
        parameterTypes: Array<Class<*>>
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

    @Suppress("UNCHECKED_CAST")
    fun findConstructorExact(
        clazz: Class<*>,
        parameterTypes: Array<Class<*>>
    ): java.lang.reflect.Constructor<*>? {
        return try {
            findConstructorMethod?.invoke(
                null,
                clazz,
                parameterTypes
            ) as? java.lang.reflect.Constructor<*>
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun hookMethod(
        clazz: Class<*>,
        methodName: String,
        parameterTypes: Array<Class<*>>,
        callback: MethodHookCallback
    ) {
        try {
            val hookerClass = methodHookClass?.classLoader?.loadClass("de.robv.android.xposed.XposedHelpers\$MethodHooker")
            val hookerConstructor = hookerClass?.getDeclaredConstructor(methodHookClass)
            val hooker = hookerConstructor?.newInstance(callback)

            val args = arrayOfNulls<Any>(parameterTypes.size + 3)
            args[0] = clazz
            args[1] = methodName
            for (i in parameterTypes.indices) {
                args[i + 2] = parameterTypes[i]
            }
            args[args.size - 1] = hooker

            hookMethodMethod?.invoke(null, *args)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun hookConstructor(
        clazz: Class<*>,
        parameterTypes: Array<Class<*>>,
        callback: MethodHookCallback
    ) {
        try {
            val hookerClass = methodHookClass?.classLoader?.loadClass("de.robv.android.xposed.XposedHelpers\$MethodHooker")
            val hookerConstructor = hookerClass?.getDeclaredConstructor(methodHookClass)
            val hooker = hookerConstructor?.newInstance(callback)

            val args = arrayOfNulls<Any>(parameterTypes.size + 2)
            args[0] = clazz
            for (i in parameterTypes.indices) {
                args[i + 1] = parameterTypes[i]
            }
            args[args.size - 1] = hooker

            hookConstructorMethod?.invoke(null, *args)
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
        private val method: Method?,
        private val hooker: Any?
    ) {
        private var resultValue: Any? = null
        private var thrownValue: Throwable? = null

        var result: Any?
            get() = try {
                resultField?.get(hooker)
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
            get() = thrownValue
            set(value) {
                thrownValue = value
                try {
                    throwableField?.set(hooker, value)
                } catch (e: Exception) {
                }
            }

        private val resultField: java.lang.reflect.Field?
            get() = try {
                val field = hooker?.javaClass?.getDeclaredField("\$result")
                field?.isAccessible = true
                field
            } catch (e: Exception) {
                null
            }

        private val throwableField: java.lang.reflect.Field?
            get() = try {
                val field = hooker?.javaClass?.getDeclaredField("\$throwable")
                field?.isAccessible = true
                field
            } catch (e: Exception) {
                null
            }
    }
}
