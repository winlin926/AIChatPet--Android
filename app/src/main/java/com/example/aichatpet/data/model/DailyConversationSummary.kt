package com.example.aichatpet.data.model

data class DailyConversationSummary( // 数据类，用于在聊天历史记录页面 (Screen5) 的 RecyclerView 中展示每日对话的摘要信息
    val date: String,               // 日期字符串，例如 "2025-05-10" 或其他你选择的显示格式
    val lastMessageSnippet: String, // 当天最后一条消息的文本片段，用于预览
    val messageCount: Int,          // 当天用户与宠物之间的消息总条数
    val timestampForSorting: Long   // 用于对此摘要列表进行排序的代表性时间戳 (通常是当日最后一条消息的时间戳，以实现降序排列)
)