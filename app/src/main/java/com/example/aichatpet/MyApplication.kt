package com.example.aichatpet

import android.app.Application
import com.example.aichatpet.data.local.AppDatabase // 导入应用中定义的AppDatabase类

class MyApplication : Application() { // 自定义的Application类，用于执行应用级别的初始化操作和管理全局状态

    // 使用 'by lazy' 属性委托来延迟初始化AppDatabase实例。
    // 这确保了数据库只在第一次被访问时才会被创建，并且这个创建过程是线程安全的，保证了单例。
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) } // 提供一个全局可访问的AppDatabase实例

    override fun onCreate() { // Application的onCreate方法，在应用程序启动并创建Application对象时调用
        super.onCreate() // 必须调用父类的onCreate方法
        // 此处可以放置需要在应用启动时立即执行的其他全局初始化代码。
        // 例如：初始化第三方库、设置全局错误处理器、预加载一些轻量级配置等。
        // 对于 'database' 属性，由于使用了lazy初始化，它会在首次被其他组件（如ViewModel或Repository）访问时自动创建。
        // 因此，通常不需要在这里显式调用 'database' 来触发创建，除非有特殊需要在应用启动时就确保数据库已连接。
    }
}