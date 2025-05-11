package com.example.aichatpet.data.network

import com.example.aichatpet.data.remote.MoonshotApiService // Moonshot 服务接口
// import com.example.aichatpet.data.remote.BaiduApiService // 如果您回退到包含百度的版本，取消注释
import com.example.aichatpet.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient { // 单例对象，用于创建和管理 Retrofit API 服务实例

    // private const val BAIDU_BASE_URL = "https://aip.baidubce.com/" // 如果回退到包含百度的版本，取消注释
    private const val MOONSHOT_BASE_URL = "https://api.moonshot.cn/" // Moonshot (Kimi AI) API的基础URL

    private val loggingInterceptor = HttpLoggingInterceptor().apply { // HTTP请求/响应日志拦截器
        level = if (BuildConfig.DEBUG) { // 根据构建类型设置日志级别
            HttpLoggingInterceptor.Level.BODY // DEBUG模式下记录详细日志
        } else {
            HttpLoggingInterceptor.Level.NONE // RELEASE模式下不记录日志
        }
    }

    private val okHttpClient = OkHttpClient.Builder() // 配置并构建共享的 OkHttpClient 实例
        .addInterceptor(loggingInterceptor) // 添加日志拦截器
        .connectTimeout(30, TimeUnit.SECONDS) // 连接超时30秒
        .readTimeout(30, TimeUnit.SECONDS) // 读取超时30秒
        .writeTimeout(30, TimeUnit.SECONDS) // 写入超时30秒
        .build()

    // val baiduApiService: BaiduApiService by lazy { // 如果回退到包含百度的版本，取消注释
    //     Retrofit.Builder()
    //         .baseUrl(BAIDU_BASE_URL)
    //         .client(okHttpClient)
    //         .addConverterFactory(GsonConverterFactory.create())
    //         .build()
    //         .create(BaiduApiService::class.java)
    // }

    val moonshotApiService: MoonshotApiService by lazy { // 懒加载方式创建 MoonshotApiService 实例
        Retrofit.Builder() // 构建 Retrofit 实例
            .baseUrl(MOONSHOT_BASE_URL) // 设置Moonshot API的基础URL
            .client(okHttpClient) // 使用共享的 OkHttpClient
            .addConverterFactory(GsonConverterFactory.create()) // 添加 Gson 转换器
            .build() // 完成构建
            .create(MoonshotApiService::class.java) // 创建 MoonshotApiService 接口的实现
    }
}