package com.example.aichatpet.data.model.moonshot

import com.google.gson.annotations.SerializedName

data class MoonshotResponse(
    val id: String?,
    val choices: List<Choice>?,
    val error: MoonshotError? = null // API 返回错误时会有这个字段
)

data class Choice( // 这个 Choice 是 MoonshotResponse 内部的
    val message: MoonshotMessage // 引用了上面定义的 MoonshotMessage
)

data class MoonshotError(
    val message: String?,
    val type: String?,
    @SerializedName("param") // API JSON字段为 "param"
    val parameter: String?,
    val code: String?
)