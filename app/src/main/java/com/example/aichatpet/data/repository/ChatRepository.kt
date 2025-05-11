package com.example.aichatpet.data.repository

import com.example.aichatpet.data.network.ApiClient
import android.util.Log
import com.example.aichatpet.data.local.ChatMessageDao
import com.example.aichatpet.data.model.ChatMessage
import com.example.aichatpet.data.remote.dto.ApiMessage
import com.example.aichatpet.data.remote.dto.ChatCompletionRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatRepository(private val chatMessageDao: ChatMessageDao) { // 负责处理聊天相关的业务逻辑，包括与网络API的交互和本地数据库的读写

    private val moonshotApiService = ApiClient.moonshotApiService // 从ApiClient获取Moonshot API服务的单例

    suspend fun getPetReplyFromNetwork( // 从Moonshot API获取AI宠物的回复
        userApiKey: String, // 用户提供的API密钥
        conversationHistory: List<ApiMessage>, // 最近的对话历史列表 (ApiMessage格式)，应由ViewModel构建完整，包含用户最新消息和之前的上下文
        petName: String // 当前的宠物名称，用于动态构建系统提示
    ): String { // 返回类型为 String，此方法保证返回一个非空字符串（即使是错误信息）
        val apiMessages = mutableListOf<ApiMessage>() // 初始化将发送给API的消息列表
        apiMessages.add(ApiMessage(role = "system", content = "你是一只可爱的AI宠物，名叫${petName}，会用友好和俏皮的语气回复。")) // 使用动态宠物名构建系统提示
        apiMessages.addAll(conversationHistory) // 添加由ViewModel准备好的、包含用户最新消息的完整对话历史

        val request = ChatCompletionRequest( // 构建API请求体对象
            model = "moonshot-v1-8k", // 指定使用的AI模型 (或从配置中读取)
            messages = apiMessages, // 传入构建好的包含动态系统提示和完整对话历史的消息列表
            temperature = 0.7, // 设置回复的随机性，0.7表示较为创新和多样
        )
        return withContext(Dispatchers.IO) { // 将网络请求操作切换到IO线程执行，避免阻塞UI线程
            try {
                val authorizationHeader = if (!userApiKey.startsWith("Bearer ")) { // 检查用户提供的API Key是否已包含"Bearer "前缀
                    "Bearer $userApiKey" // 如果没有，则添加前缀
                } else {
                    userApiKey // 如果已有，则直接使用
                }
                val response = moonshotApiService.getChatCompletions( // 调用Moonshot API服务获取聊天补全结果
                    apiKey = authorizationHeader, // 传入处理过的授权头部信息
                    contentType = "application/json", // 显式指定请求的内容类型为JSON
                    requestBody = request // 传入构建好的请求体
                )
                if (response.isSuccessful) { // 检查HTTP响应状态码是否表示成功 (例如 200-299)
                    val responseBody = response.body() // 获取响应体内容
                    val petReply = (responseBody?.choices?.firstOrNull()?.message?.content as? String)?.trim()

                    // 使用 !.isNullOrBlank() 来检查 petReply 是否为 null、空字符串或仅包含空白字符
                    if (!petReply.isNullOrBlank()) {
                        Log.d("ChatRepository", "API Success: $petReply") // 记录成功的日志
                        petReply // 如果 petReply 有效，它已经是 String 类型，直接返回
                    } else { // 如果响应成功但未能解析到具体回复内容或内容为空白
                        Log.e("ChatRepository", "API Success but no valid content: ${responseBody?.error?.message}") // 记录错误日志
                        responseBody?.error?.let { // 尝试从响应体中解析API返回的错误信息
                            "API Error: ${it.message} (Type: ${it.type}, Code: ${it.code})" // 返回格式化的API错误信息
                        } ?: "未能获取有效的回复内容。" // 如果连API错误信息也无法解析或内容为空，则返回通用提示
                    }
                } else { // 如果HTTP响应状态码表示失败 (例如 4xx客户端错误, 5xx服务端错误)
                    val errorBody = response.errorBody()?.string() // 尝试获取原始的错误响应体字符串
                    Log.e("ChatRepository", "API Error: ${response.code()} - $errorBody") // 记录详细的API错误日志
                    "API 请求失败: ${response.message()} (Code: ${response.code()})" // 构建并返回一个表示API请求失败的错误消息，包含状态码和消息
                }
            } catch (e: Exception) { // 捕获在网络请求或响应处理过程中可能发生的任何其他异常 (如网络连接问题、超时、解析错误等)
                Log.e("ChatRepository", "Network/Unknown Error: ${e.message}", e) // 记录异常信息
                "网络错误，请稍后再试。" // 向用户返回一个通用的网络错误提示
            }
        }
    }

    @Suppress("unused") // 暂时抑制“未使用函数”的警告，如果确实不需要此功能，后续可以删除
    suspend fun getAllMessagesFromDb(): List<ChatMessage> { // 从本地Room数据库获取所有已存储的聊天记录
        return withContext(Dispatchers.IO) { // 将数据库操作切换到IO线程执行
            chatMessageDao.getAllMessages() // 调用DAO方法获取所有消息
        }
    }

    suspend fun insertMessageToDb(message: ChatMessage) { // 将一条聊天消息插入到本地Room数据库
        withContext(Dispatchers.IO) { // 将数据库操作切换到IO线程执行
            chatMessageDao.insertMessage(message) // 调用DAO方法插入消息
        }
    }

    suspend fun deleteAllMessagesFromDb() { // 删除本地Room数据库中的所有聊天记录
        withContext(Dispatchers.IO) { // 将数据库操作切换到IO线程执行
            chatMessageDao.deleteAllMessages() // 调用DAO方法删除所有消息
        }
    }

    suspend fun getDistinctChatDatesFromDb(): List<String> { // 从数据库获取所有唯一的聊天日期字符串 (格式úrg-MM-DD)，用于历史记录按日期分组展示
        return withContext(Dispatchers.IO) { // 数据库查询，切换到IO线程
            chatMessageDao.getDistinctChatDates()
        }
    }

    suspend fun getMessagesByDateFromDb(dateString: String): List<ChatMessage> { // 根据指定的日期字符串 (格式úrg-MM-DD) 从数据库获取该日期的所有聊天消息
        return withContext(Dispatchers.IO) { // 数据库查询，切换到IO线程
            chatMessageDao.getMessagesByDate(dateString)
        }
    }

    suspend fun getLastMessageByDateFromDb(dateString: String): ChatMessage? { // 根据指定的日期字符串 (格式úrg-MM-DD) 从数据库获取该日期的最后一条聊天消息
        return withContext(Dispatchers.IO) { // 数据库查询，切换到IO线程
            chatMessageDao.getLastMessageByDate(dateString)
        }
    }

    suspend fun deleteMessagesByDateFromDb(dateString: String): Int { // 根据指定的日期字符串 (格式úrg-MM-DD) 删除该日期的所有聊天消息，并返回成功删除的条数
        return withContext(Dispatchers.IO) { // 数据库删除操作，切换到IO线程
            chatMessageDao.deleteMessagesByDate(dateString)
        }
    }
}