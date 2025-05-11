package com.example.aichatpet.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.aichatpet.MyApplication
import com.example.aichatpet.data.model.DailyConversationSummary
import com.example.aichatpet.data.repository.ChatRepository
import kotlinx.coroutines.launch

class ChatHistoryViewModel(application: Application) : AndroidViewModel(application) { // 聊天历史界面的ViewModel，继承自AndroidViewModel以方便访问Application上下文

    private val repository: ChatRepository // ChatRepository的实例，用于与数据层(数据库和网络)交互

    private val _dailySummaries = MutableLiveData<List<DailyConversationSummary>>() // 私有的、可变的LiveData，用于存储每日对话摘要列表
    val dailySummaries: LiveData<List<DailyConversationSummary>> = _dailySummaries // 公开的、不可变的LiveData，UI层通过它观察每日对话摘要列表的变化

    private val _isLoading = MutableLiveData<Boolean>(false) // 私有的、可变的LiveData，用于表示数据加载状态，初始值为false (未加载)
    val isLoading: LiveData<Boolean> = _isLoading // 公开的LiveData，UI层通过它观察加载状态的变化，以显示/隐藏进度条等

    private val _isEmpty = MutableLiveData<Boolean>(true) // 私有的、可变的LiveData，用于表示聊天历史记录是否为空，初始假设为空 (true)
    val isEmpty: LiveData<Boolean> = _isEmpty // 公开的LiveData，UI层通过它观察历史记录是否为空，以显示“无记录”提示等

    private val _errorMessage = MutableLiveData<String?>() // 私有的、可变的LiveData，用于存储发生的错误消息文本 (可为null)
    val errorMessage: LiveData<String?> = _errorMessage // 公开的LiveData，UI层通过它观察错误消息，以便向用户显示提示

    init { // ViewModel的初始化块，在ViewModel实例首次创建时执行
        val chatMessageDao = (application as MyApplication).database.chatMessageDao() // 从Application实例中获取数据库并进一步获取ChatMessageDao的实例
        repository = ChatRepository(chatMessageDao) // 使用获取到的DAO初始化ChatRepository
        loadChatHistorySummaries() // ViewModel创建时，立即调用方法加载聊天历史摘要数据
    }

    fun loadChatHistorySummaries() { // 公开方法，用于加载或刷新聊天历史的每日摘要信息
        viewModelScope.launch { // 在viewModelScope中启动一个协程，确保协程在ViewModel销毁时自动取消，避免内存泄漏
            _isLoading.value = true // 在开始加载数据前，将加载状态设置为true
            _errorMessage.value = null // 清除之前可能存在的错误消息
            try { // 使用try-catch块来捕获数据加载过程中可能发生的异常
                val distinctDates = repository.getDistinctChatDatesFromDb() // 从仓库获取所有唯一的聊天日期字符串列表 (通常已按降序排列)
                if (distinctDates.isEmpty()) { // 如果没有获取到任何日期，说明聊天记录为空
                    _dailySummaries.value = emptyList() // 将每日摘要列表设置为空列表
                    _isEmpty.value = true // 将历史记录为空的状态设置为true
                } else { // 如果获取到了聊天日期
                    val summaries = mutableListOf<DailyConversationSummary>() // 创建一个可变的列表，用于存放生成的每日摘要对象
                    for (dateStr in distinctDates) { // 遍历每一个唯一的日期字符串
                        val lastMessage = repository.getLastMessageByDateFromDb(dateStr) // 获取该日期的最后一条消息，用于生成摘要和排序依据
                        val messagesOnDate = repository.getMessagesByDateFromDb(dateStr) // 获取该日期的所有消息，主要用于统计消息数量

                        if (lastMessage != null) { // 确保该日期确实有最后一条消息 (理论上如果日期存在，消息也应存在)
                            summaries.add( // 创建一个DailyConversationSummary对象并添加到列表中
                                DailyConversationSummary(
                                    date = dateStr, // 设置日期
                                    lastMessageSnippet = lastMessage.text.take(50) + if (lastMessage.text.length > 50) "..." else "", // 从最后一条消息文本中截取前50个字符作为摘要，如果超过50字符则添加省略号
                                    messageCount = messagesOnDate.size, // 设置该日期的消息总数
                                    timestampForSorting = lastMessage.timestamp // 使用该日期最后一条消息的时间戳作为排序依据 (确保摘要列表按日期新旧正确排列)
                                )
                            )
                        }
                    }
                    _dailySummaries.value = summaries // 将生成的每日摘要列表更新到LiveData
                    _isEmpty.value = summaries.isEmpty() // 根据生成的摘要列表是否为空来更新历史记录为空的状态 (通常此时应为false)
                }
            } catch (e: Exception) { // 如果在try块中发生任何异常
                Log.e("ChatHistoryVM", "Error loading chat history summaries: ${e.message}", e) // 记录详细的错误日志
                _errorMessage.value = "加载聊天记录失败: ${e.message}" // 将错误消息设置到LiveData，以便UI层提示用户
                _dailySummaries.value = emptyList() // 发生错误时，将每日摘要列表清空
                _isEmpty.value = true // 并将历史记录为空的状态设置为true
            } finally { // finally块中的代码无论是否发生异常都会执行
                _isLoading.value = false // 数据加载（无论成功或失败）完成后，将加载状态恢复为false
            }
        }
    }

    fun deleteConversationByDate(dateString: String) { // 公开方法，用于删除指定日期的所有聊天记录
        viewModelScope.launch { // 在viewModelScope中启动协程执行删除操作
            _isLoading.value = true // 开始删除前，可以将加载状态设置为true，以向用户反馈操作正在进行
            try { // 使用try-catch块处理删除过程中可能发生的异常
                val deletedRows = repository.deleteMessagesByDateFromDb(dateString) // 调用仓库方法执行删除操作，并获取删除的行数
                Log.d("ChatHistoryVM", "Deleted $deletedRows messages for date: $dateString") // 记录删除操作的日志
                loadChatHistorySummaries() // 删除成功后，立即重新加载聊天历史摘要列表，以更新UI显示
            } catch (e: Exception) { // 如果删除过程中发生异常
                Log.e("ChatHistoryVM", "Error deleting conversation for date $dateString: ${e.message}", e) // 记录详细的错误日志
                _errorMessage.value = "删除聊天记录失败: ${e.message}" // 将错误消息设置到LiveData
            } finally { // 无论删除成功或失败，最终都执行
                _isLoading.value = false // 将加载状态恢复为false
            }
        }
    }

    fun clearErrorMessage() { // 公开方法，用于清除当前的错误消息状态
        _errorMessage.value = null // 将错误消息LiveData的值设置为null，通常在UI层显示完错误提示后调用此方法
    }
}