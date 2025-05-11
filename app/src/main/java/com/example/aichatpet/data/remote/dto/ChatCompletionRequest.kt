package com.example.aichatpet.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChatCompletionRequest( // 发送给聊天补全 (Chat Completions) API 的请求体结构
    @SerializedName("model")
    val model: String, // 指定进行聊天的 AI 模型 ID (例如 "moonshot-v1-8k", "moonshot-v1-32k" 等)
    @SerializedName("messages")
    val messages: List<ApiMessage>, // 包含上下文消息的列表，按对话顺序排列，最近一条消息在最后
    @SerializedName("temperature")
    val temperature: Double? = null, // (可选) 控制输出的随机性：值越高输出越随机 (如0.8)，值越低输出越确定 (如0.2)。API服务端有默认值。
    @SerializedName("max_tokens")
    val maxTokens: Int? = null, // (可选) 指定模型生成内容的最大 token 数量，用于控制输出长度和API使用成本
    @SerializedName("stream")
    val stream: Boolean? = false // (可选) 是否启用流式传输，若为 true，响应将作为 server-sent events 分块返回。默认为 false。
)