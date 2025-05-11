package com.example.aichatpet.data.model.moonshot

data class MoonshotRequest(
    val model: String = "moonshot-v1-8k",
    val messages: List<MoonshotMessage>,
    val temperature: Float = 0.3f // 控制生成文本的随机性
)

data class MoonshotMessage(
    val role: String, // 通常是 "system", "user", 或 "assistant"
    val content: String
)