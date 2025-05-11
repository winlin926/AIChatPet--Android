package com.example.aichatpet.data.remote

import com.example.aichatpet.data.remote.dto.ChatCompletionRequest
import com.example.aichatpet.data.remote.dto.ChatCompletionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface MoonshotApiService { // Retrofit 服务接口，定义了与 Moonshot AI API 进行 HTTP 通信的方法

    companion object { // 存放与此服务接口相关的常量，如API端点路径和HTTP头部名称
        const val ENDPOINT_CHAT_COMPLETIONS = "v1/chat/completions" // Moonshot 聊天补全功能的API端点路径
        const val HEADER_AUTHORIZATION = "Authorization" // HTTP 授权头部字段名
        const val HEADER_CONTENT_TYPE = "Content-Type" // HTTP 内容类型头部字段名
    }

    @POST(ENDPOINT_CHAT_COMPLETIONS) // 声明这是一个 HTTP POST 请求，路径由 ENDPOINT_CHAT_COMPLETIONS 指定
    suspend fun getChatCompletions( // 异步调用 Moonshot AI 的聊天补全 API
        @Header(HEADER_AUTHORIZATION) apiKey: String, // API 密钥，将作为 "Authorization" HTTP头部发送 (通常格式为 "Bearer YOUR_API_KEY")
        @Header(HEADER_CONTENT_TYPE) contentType: String = "application/json", // 请求的内容类型，将作为 "Content-Type" HTTP头部发送，默认为 "application/json"
        @Body requestBody: ChatCompletionRequest // 请求体，包含模型名称、消息列表等信息，将被序列化为JSON发送
    ): Response<ChatCompletionResponse> // 返回一个 Retrofit 的 Response 对象，包装了服务器的响应 (成功时为ChatCompletionResponse，失败时可访问错误体)
}