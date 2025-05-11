package com.example.aichatpet.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChatCompletionResponse( // 聊天补全 (Chat Completions) API 返回的响应体顶层结构
    @SerializedName("id")
    val id: String?, // 本次聊天补全请求的唯一标识符 (例如 "cmpl-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
    @SerializedName("object")
    val objectType: String?, // 响应对象的类型 (例如 "chat.completion")，原JSON字段为 "object" (因object是Kotlin关键字故改名)
    @SerializedName("created")
    val created: Long?, // 聊天补全请求创建时的Unix时间戳 (自1970-01-01T00:00:00Z起的秒数)
    @SerializedName("model")
    val model: String?, // 本次请求所使用的具体AI模型的ID
    @SerializedName("choices")
    val choices: List<Choice>?, // AI 生成的回复选项列表，在非流式模式下通常只包含一个元素
    @SerializedName("usage")
    val usage: UsageStats?, // 本次请求的 token 使用情况统计 (仅在非流式模式的最后一条消息中出现)
    @SerializedName("error")
    val error: ApiError? = null // 如果请求处理出错，则此字段包含错误详情对象；若请求成功，则此字段为 null
)

data class Choice( // 代表 AI 模型生成的一个具体的回复选项
    @SerializedName("index")
    val index: Int?, // 该选项在 choices 列表中的索引，通常从 0 开始
    @SerializedName("message")
    val message: ApiMessage?, // AI 生成的具体消息内容，其结构复用了我们定义的 ApiMessage
    @SerializedName("finish_reason")
    val finishReason: String? // AI停止生成内容的原因 (例如 "stop": 自然结束, "length": 达到max_tokens限制, "content_filter": 内容被过滤)
)

data class UsageStats( // 描述 API 调用中 token 使用量的统计信息
    @SerializedName("prompt_tokens")
    val promptTokens: Int?, // 输入提示 (prompt messages) 部分所消耗的 token 数量
    @SerializedName("completion_tokens")
    val completionTokens: Int?, // AI 生成回复 (completion) 部分所消耗的 token 数量
    @SerializedName("total_tokens")
    val totalTokens: Int? // 本次请求总共消耗的 token 数量 (通常是 prompt_tokens + completion_tokens)
)

data class ApiError( // API 错误响应的通用数据结构 (具体字段可能因API而异，请参考实际API文档)
    @SerializedName("message")
    val message: String?, // 人类可读的、关于错误的详细描述信息
    @SerializedName("type")
    val type: String?, // 错误的类型 (例如 "invalid_request_error", "authentication_error")
    @SerializedName("param")
    val param: String?, // 如果错误与发送请求中的某个特定参数相关，则此字段会指明该参数的名称
    @SerializedName("code")
    val code: String? // API 定义的错误代码字符串 (也可能是数字或null，具体取决于API的设计)
)