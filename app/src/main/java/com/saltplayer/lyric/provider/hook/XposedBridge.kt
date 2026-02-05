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

            val arrayClass = Array<Class<*>>::class.java
            findMethodMethod = xposedHelpersClass?.getMethod(
                "findMethodExactIfExists",
                Class::class.java,
                String::class.java,
                arrayClass
            )

            findConstructorMethod = xposedHelpersClass?.getMethod(
                "findConstructorExactIfExists",
                Class::class.java,
                arrayClass
            )

            val anyArrayClass = Array<Any>::class.java
            hookMethodMethod = xposedHelpersClass?.getMethod(
                "findAndHookMethod",
                Class::class.java,
                String::class.java,
                anyArrayClass,
                methodHookClass
            )

            hookConstructorMethod = xposedHelpersClass?.getMethod(
                "findAndHookConstructor",
                Class::class.java,
                anyArrayClass,
                methodHookClass
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

    fun findMethodExact(
        clazz: Class<*>,
        methodName: String,
        parameterTypes: Array<Class<*>>
    ): Method? {
        return try {
            @Suppress("UNCHECKED_CAST")
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

    fun findMethodExactIfExists(
        clazz: Class<*>,
        methodName: String,
        vararg parameterTypes: Class<*>
    ): Method? {
        val arrayClass = Array<Class<*>>::class.java
        val array = java.lang.reflect.Array.newInstance(parameterTypes.javaClass.componentType, parameterTypes.size) as Array<Class<*>>
        for (i in parameterTypes.indices) {
            array[i] = parameterTypes[i]
        }
        return findMethodExact(clazz, methodName, array)
    }

    fun findConstructorExact(
        clazz: Class<*>,
        parameterTypes: Array<Class<*>>
    ): java.lang.reflect.Constructor<*>? {
        return try {
            @Suppress("UNCHECKED_CAST")
            findConstructorMethod?.invoke(
                null,
                clazz,
                parameterTypes
            ) as? java.lang.reflect.Constructor<*>
        } catch (e: Exception) {
            null
        }
    }

    fun findConstructorExactIfExists(
        clazz: Class<*>,
        vararg parameterTypes: Class<*>
    ): java.lang.reflect.Constructor<*>? {
        val arrayClass = Array<Class<*>>::class.java
        val array = java.lang.reflect.Array.newInstance(parameterTypes.javaClass.componentType, parameterTypes.size) as Array<Class<*>>
        for (i in parameterTypes.indices) {
            array[i] = parameterTypes[i]
        }
        return findConstructorExact(clazz, array)
    }

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

            val anyArrayClass = Array<Any>::class.java
            val args = java.lang.reflect.Array.newInstance(Any::class.java, parameterTypes.size + 2) as Array<Any?>
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

    fun hookConstructor(
        clazz: Class<*>,
        parameterTypes: Array<Class<*>>,
        callback: MethodHookCallback
    ) {
        try {
            val hookerClass = methodHookClass?.classLoader?.loadClass("de.robv.android.xposed.XposedHelpers\$MethodHooker")
            val hookerConstructor = hookerClass?.getDeclaredConstructor(methodHookClass)
            val hooker = hookerConstructor?.newInstance(callback)

            val anyArrayClass = Array<Any>::class.java
            val args = java.lang.reflect.Array.newInstance(Any::class.java, parameterTypes.size + 1) as Array<Any?>
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
