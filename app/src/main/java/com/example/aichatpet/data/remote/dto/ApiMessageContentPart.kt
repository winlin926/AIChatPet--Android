package com.example.aichatpet.data.remote.dto // 确保包名与你的项目结构一致

import com.google.gson.annotations.SerializedName

// 用于表示 Kimi Vision API 中 message.content 数组里的一个元素（文本或图片URL）
sealed class ApiMessageContentPart { // 密封类，定义了 content 中可能出现的元素类型
    abstract val type: String // 每个元素都有一个 "type" 字段，指明其类型
}

data class TextContentPart( // 代表文本内容元素
    val text: String // 文本的具体内容
) : ApiMessageContentPart() { // 继承自 ApiMessageContentPart
    @SerializedName("type") // 确保Gson序列化时，JSON对象中的字段名为 "type"
    override val type: String = "text" // 类型固定为 "text"
}

data class ImageUrl( // 代表图片URL对象，它会被嵌套在 ImageUrlContentPart 中
    val url: String // 包含 "data:image/[mimetype];base64,[base64_encoded_image_data]" 格式的图片数据URL
)

data class ImageUrlContentPart( // 代表图片URL内容元素
    @SerializedName("image_url") // JSON字段名为 "image_url"
    val imageUrl: ImageUrl // 该字段的值是一个 ImageUrl 对象
) : ApiMessageContentPart() { // 继承自 ApiMessageContentPart
    @SerializedName("type") // 确保Gson序列化时，JSON对象中的字段名为 "type"
    override val type: String = "image_url" // 类型固定为 "image_url"
}