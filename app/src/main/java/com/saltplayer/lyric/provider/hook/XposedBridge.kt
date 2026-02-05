package com.saltplayer.lyric.provider.hook

import java.lang.reflect.Method

object XposedBridge {
    private var xposedHelpersClass: Class<*>? = null
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
            methodHookClass = classLoader.loadClass("de.robv.android.xposed.callbacks.XC_MethodHook")

            findClassMethod = xposedHelpersClass!!.getMethod(
                "findClassIfExists",
                String::class.java,
                ClassLoader::class.java
            )

            findMethodMethod = xposedHelpersClass!!.getMethod(
                "findMethodExactIfExists",
                Class::class.java,
                String::class.java,
                Array<Class<*>>::class.java
            )

            findConstructorMethod = xposedHelpersClass!!.getMethod(
                "findConstructorExactIfExists",
                Class::class.java,
                Array<Class<*>>::class.java
            )

            hookMethodMethod = xposedHelpersClass!!.getMethod(
                "findAndHookMethod",
                Class::class.java,
                String::class.java,
                Array<Any>::class.java,
                methodHookClass
            )

            hookConstructorMethod = xposedHelpersClass!!.getMethod(
                "findAndHookConstructor",
                Class::class.java,
                Array<Any>::class.java,
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
            findMethodMethod?.invoke(null, clazz, methodName, parameterTypes) as? Method
        } catch (e: Exception) {
            null
        }
    }

    fun findConstructorExact(
        clazz: Class<*>,
        parameterTypes: Array<Class<*>>
    ): java.lang.reflect.Constructor<*>? {
        return try {
            findConstructorMethod?.invoke(null, clazz, parameterTypes) as? java.lang.reflect.Constructor<*>
        } catch (e: Exception) {
            null
        }
    }

    fun hookMethod(
        clazz: Class<*>,
        methodName: String,
        parameterTypes: Array<Class<*>>,
        callback: MethodHookCallback
    ) {
        try {
            val hookerClass = methodHookClass!!.classLoader.loadClass("de.robv.android.xposed.XposedHelpers\$MethodHooker")
            val hookerConstructor = hookerClass.getDeclaredConstructor(methodHookClass)
            val hooker = hookerConstructor.newInstance(callback)

            val argsSize = parameterTypes.size + 3
            val args = java.lang.reflect.Array.newInstance(Any::class.java, argsSize) as Array<Any?>
            args[0] = clazz
            args[1] = methodName
            for (i in parameterTypes.indices) {
                args[i + 2] = parameterTypes[i]
            }
            args[argsSize - 1] = hooker

            hookMethodMethod!!.invoke(null, *args)
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
            val hookerClass = methodHookClass!!.classLoader.loadClass("de.robv.android.xposed.XposedHelpers\$MethodHooker")
            val hookerConstructor = hookerClass.getDeclaredConstructor(methodHookClass)
            val hooker = hookerConstructor.newInstance(callback)

            val argsSize = parameterTypes.size + 2
            val args = java.lang.reflect.Array.newInstance(Any::class.java, argsSize) as Array<Any?>
            args[0] = clazz
            for (i in parameterTypes.indices) {
                args[i + 1] = parameterTypes[i]
            }
            args[argsSize - 1] = hooker

            hookConstructorMethod!!.invoke(null, *args)
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
        private val hooker: Any?
    ) {
        private var resultValue: Any? = null
        private var thrownValue: Throwable? = null

        var result: Any?
            get() = resultValue
            set(value) {
                resultValue = value
            }

        var throwable: Throwable?
            get() = thrownValue
            set(value) {
                thrownValue = value
            }
    }
}
