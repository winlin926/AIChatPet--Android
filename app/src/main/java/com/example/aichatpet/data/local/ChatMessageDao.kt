package com.example.aichatpet.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.aichatpet.data.model.ChatMessage

@Dao
interface ChatMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMessages(messages: List<ChatMessage>)

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    suspend fun getAllMessages(): List<ChatMessage>

    @Query("SELECT * FROM chat_messages WHERE message_id = :messageId")
    suspend fun getMessageById(messageId: String): ChatMessage?

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()

    /**
     * 获取所有有聊天记录的日期（格式YYYY-MM-DD），按日期降序排列。
     * strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch') 用于将 Unix 时间戳转换为日期字符串。
     * 我们选取每个日期的最大时间戳 (MAX(timestamp)) 来确保即使同一天有多条消息，也只返回一个日期条目，并用于排序。
     */
    @Query("""
        SELECT strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch') as chat_date
        FROM chat_messages
        GROUP BY chat_date
        ORDER BY MAX(timestamp) DESC
    """)
    suspend fun getDistinctChatDates(): List<String>

    /**
     * 获取指定日期的所有聊天消息，按时间戳升序排列。
     * @param dateString 日期字符串，格式为 'YYYY-MM-DD'
     */
    @Query("SELECT * FROM chat_messages WHERE strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch') = :dateString ORDER BY timestamp ASC")
    suspend fun getMessagesByDate(dateString: String): List<ChatMessage>

    /**
     * 获取指定日期的最后一条消息。
     * @param dateString 日期字符串，格式为 'YYYY-MM-DD'
     */
    @Query("SELECT * FROM chat_messages WHERE strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch') = :dateString ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessageByDate(dateString: String): ChatMessage?

    /**
     * 删除指定日期的所有聊天消息。
     * @param dateString 日期字符串，格式为 'YYYY-MM-DD'
     * @return 返回删除的行数
     */
    @Query("DELETE FROM chat_messages WHERE strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch') = :dateString")
    suspend fun deleteMessagesByDate(dateString: String): Int
}