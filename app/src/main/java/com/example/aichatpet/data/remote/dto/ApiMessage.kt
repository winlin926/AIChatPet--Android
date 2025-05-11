package com.example.aichatpet.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ApiMessage( // 用于API请求和响应中表示单条消息的数据结构
    @SerializedName("role")
    val role: String, // 消息发送者的角色，例如 "system" (系统), "user" (用户), 或 "assistant" (AI助手)
    @SerializedName("content")
    val content: Any // <<< 修改：类型改为 Any。当用于Kimi Vision时，这里会传入 List<ApiMessageContentPart>；用于普通聊天时，传入 String。
    // Gson在序列化时会根据传入对象的实际类型进行处理。
)