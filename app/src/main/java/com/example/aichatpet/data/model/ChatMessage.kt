package com.example.aichatpet.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

enum class SenderType { // 定义枚举类以区分消息发送者
    USER, PET
}

class SenderTypeConverter { // Room 类型转换器，用于在 SenderType 枚举与其数据库存储的字符串表示之间进行转换
    @TypeConverter
    fun fromSenderType(value: SenderType): String {
        return value.name // 将 SenderType 枚举转换为其名称字符串 (例如 "USER", "PET") 进行存储
    }

    @TypeConverter
    fun toSenderType(value: String): SenderType {
        return SenderType.valueOf(value) // 从数据库读取字符串时，将其转换回 SenderType 枚举
    }
}

@Entity(tableName = "chat_messages") // 定义此类映射到数据库中的 "chat_messages" 表
@TypeConverters(SenderTypeConverter::class) // 为此实体应用 SenderTypeConverter，以便 Room 正确处理 SenderType 枚举字段
data class ChatMessage(
    @PrimaryKey
    @ColumnInfo(name = "message_id")
    val id: String, // 消息的唯一ID，通常是一个UUID字符串，作为表的主键

    @ColumnInfo(name = "text_content")
    val text: String, // 消息的文本内容

    @ColumnInfo(name = "sender_type")
    val senderType: SenderType, // 消息的发送者类型，使用 SenderType 枚举 (USER 或 PET)

    @ColumnInfo(name = "timestamp")
    val timestamp: Long, // 消息发送时的Unix时间戳 (自1970-01-01T00:00:00Z起的毫秒数)

    @ColumnInfo(name = "chat_date") // 数据库中的列名为 "chat_date"
    val date: String // 消息发送的日期字符串，格式为 "YYYY-MM-DD"，用于按日期快速检索和分组
)