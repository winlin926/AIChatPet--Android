package com.example.aichatpet.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.aichatpet.BuildConfig
import com.example.aichatpet.MyApplication
import com.example.aichatpet.data.model.ChatMessage
import com.example.aichatpet.data.model.SenderType
import com.example.aichatpet.data.remote.dto.ApiMessage
import com.example.aichatpet.data.repository.ChatRepository
import com.example.aichatpet.ui.auth.RegisterActivity
import com.example.aichatpet.ui.settings.ApiConfigActivity
import com.example.aichatpet.ui.settings.SettingsActivity // 确保 SettingsActivity 已导入以访问其常量
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) { // 聊天界面的ViewModel，负责管理聊天消息数据、处理用户发送消息的逻辑以及与数据仓库(Repository)的交互

    private val repository: ChatRepository // ChatRepository的实例，用于与数据层(数据库和网络API)进行通信

    private val userPrefs: SharedPreferences = application.getSharedPreferences( // 用于存储用户相关偏好设置 (如宠物名) 的SharedPreferences实例
        RegisterActivity.PREFS_NAME, // 使用在RegisterActivity中定义的SharedPreferences文件名，确保数据一致性
        Context.MODE_PRIVATE // 设置访问模式为私有，只有本应用可以访问
    )
    private val apiConfigPrefs: SharedPreferences = application.getSharedPreferences( // 用于存储API相关配置 (如API密钥和端点) 的SharedPreferences实例
        ApiConfigActivity.PREFS_API_CONFIG, // 使用在ApiConfigActivity中定义的SharedPreferences文件名
        Context.MODE_PRIVATE
    )

    private val moonshotApiKey: String // 计算属性，用于获取Moonshot (Kimi AI) 的API密钥
        get() { // 每次访问此属性时会执行get代码块
            return apiConfigPrefs.getString(ApiConfigActivity.KEY_KIMI_API_KEY, null) // 首先尝试从API配置的SharedPreferences中读取Kimi API Key
                ?.takeIf { it.isNotBlank() } // 如果读取到的值不为null且不为空白字符串，则使用该值
                ?: BuildConfig.MOONSHOT_API_KEY // 否则 (如果SharedPreferences中没有或为空白)，则回退到使用BuildConfig中定义的默认API Key
        }

    private var currentPetName: String = "" // 用于存储当前宠物的名称，从SharedPreferences加载

    private val _messages = MutableLiveData(mutableListOf<ChatMessage>()) // 私有的、可变的LiveData，用于存储当前聊天会话的消息列表，初始化为空的可变列表
    val messages: LiveData<MutableList<ChatMessage>> = _messages // 公开的、不可变的LiveData，UI层通过它观察聊天消息列表的变化

    private val _isLoading = MutableLiveData(false) // 私有的、可变的LiveData，用于表示数据加载或网络请求的状态，初始值为false (未加载)
    val isLoading: LiveData<Boolean> = _isLoading // 公开的LiveData，UI层通过它观察加载状态的变化，以显示/隐藏进度指示器等

    private val _errorMessage = MutableLiveData<String?>() // 私有的、可变的LiveData，用于存储操作过程中可能发生的错误消息文本 (可为null)
    val errorMessage: LiveData<String?> = _errorMessage // 公开的LiveData，UI层通过它观察错误消息，以便向用户显示提示

    init { // ViewModel的初始化块，在ViewModel实例首次创建时执行
        val chatMessageDao = (application as MyApplication).database.chatMessageDao() // 从全局Application实例中获取数据库并进一步得到ChatMessageDao的实例
        repository = ChatRepository(chatMessageDao) // 使用获取到的DAO初始化ChatRepository
        this.currentPetName = getCurrentPetNameFromPrefs() // 从SharedPreferences加载当前宠物名称并赋值给成员变量 (使用this明确)
        Log.d("ChatViewModel", "Initialized with pet name: ${this.currentPetName} and Kimi Key (length): ${moonshotApiKey.length}") // 记录ViewModel初始化日志 (使用this)
    }

    private fun getCurrentPetNameFromPrefs(): String { // 私有辅助方法，用于从用户偏好设置中读取当前宠物名称
        return userPrefs.getString(RegisterActivity.KEY_PET_NAME, RegisterActivity.DEFAULT_PET_NAME) // 从userPrefs读取宠物名
            ?: RegisterActivity.DEFAULT_PET_NAME // 如果读取结果为null，则返回在RegisterActivity中定义的默认宠物名
    }

    fun updatePetName() { // 公开方法，当宠物名称在设置中被更改后，调用此方法来刷新ViewModel内部持有的宠物名称
        this.currentPetName = getCurrentPetNameFromPrefs() // 重新从SharedPreferences加载最新的宠物名称到成员变量 (使用this明确)
        Log.d("ChatViewModel", "Pet name updated in ViewModel to: ${this.currentPetName}") // 记录宠物名称已更新的日志 (使用this)
    }

    fun loadMessages(dateString: String? = null) { // 公开方法，用于加载指定日期 (dateString) 的聊天消息；如果dateString为null，则加载当天的消息
        val targetDateString = dateString ?: getCurrentDateString() // 确定目标日期：如果传入了dateString，则使用它；否则，获取当前日期字符串
        val petNameToUseInCoroutine = this.currentPetName // 在协程外部获取当前宠物名的副本，确保在协程中使用的是此时的值

        viewModelScope.launch { // 在viewModelScope中启动一个协程，用于执行异步的数据加载操作
            _isLoading.value = true // 在开始加载前，将加载状态设置为true
            _errorMessage.value = null // 清除之前可能存在的任何错误消息
            Log.d("ChatViewModel", "Loading messages for target date: $targetDateString. Current Pet Name is $petNameToUseInCoroutine") // 日志记录 (使用局部变量petNameToUseInCoroutine)

            try { // 使用try-catch块来捕获数据库操作或数据处理中可能发生的异常
                val messagesFromDb = repository.getMessagesByDateFromDb(targetDateString).toMutableList() // 从数据库获取对应日期的消息并转换为可变列表

                if (messagesFromDb.isEmpty() && dateString == null) { // 如果是当天且数据库没有消息，则发送初始欢迎语
                    Log.d("ChatViewModel", "No messages for today, creating initial welcome message with pet: $petNameToUseInCoroutine") // 使用局部变量
                    val welcomeMessage = ChatMessage( // 创建一条欢迎消息
                        id = UUID.randomUUID().toString(), // 为消息生成一个唯一的ID
                        text = "你好呀，我是${petNameToUseInCoroutine}！今天我们聊点什么呢？", // 包含当前宠物名称的个性化欢迎语 (使用局部变量)
                        senderType = SenderType.PET, // 将发送者类型设置为宠物
                        timestamp = System.currentTimeMillis(), // 设置当前时间为消息的时间戳
                        date = getCurrentDateString() // 设置消息的日期为当前日期
                    )
                    addMessageToLiveDataAndDb(welcomeMessage) // 这会更新 LiveData 并异步保存到数据库
                } else {
                    _messages.value = messagesFromDb // 如果有历史消息，或者不是当天，直接更新 LiveData
                }

                val petNameJustChanged = userPrefs.getBoolean(SettingsActivity.KEY_PET_NAME_JUST_CHANGED, false) // 从 userPrefs 读取“宠物名刚更改”标记

                if (petNameJustChanged) { // 如果标记为true
                    Log.d("ChatViewModel", "Pet name was just changed. Adding re-introduction for $petNameToUseInCoroutine to date: $targetDateString") // 使用局部变量
                    val reIntroductionMessage = ChatMessage( // 创建重介绍消息
                        id = UUID.randomUUID().toString(),
                        text = "你好呀，我是${petNameToUseInCoroutine}！很高兴用我的新名字和你聊天，今天想聊些什么呢？", // 使用局部变量
                        senderType = SenderType.PET,
                        timestamp = System.currentTimeMillis() + 1, // 确保时间戳略晚于已加载消息，使其显示在最后
                        date = targetDateString // 确保日期与当前聊天会话一致
                    )
                    addMessageToLiveDataAndDb(reIntroductionMessage) // 添加并保存这条重介绍消息

                    userPrefs.edit().putBoolean(SettingsActivity.KEY_PET_NAME_JUST_CHANGED, false).apply() // 清除标记，避免重复发送
                    Log.d("ChatViewModel", "Cleared KEY_PET_NAME_JUST_CHANGED flag.")
                }

            } catch (e: Exception) { // 异常处理
                Log.e("ChatViewModel", "Error loading messages for date $targetDateString: ${e.message}", e)
                _errorMessage.value = "加载聊天记录失败: ${e.message}"
                val errorLoadingMessage = ChatMessage(
                    id = UUID.randomUUID().toString(), text = "呜，我好像找不到我们的聊天记录了...",
                    senderType = SenderType.PET, timestamp = System.currentTimeMillis(), date = targetDateString
                )
                _messages.value = mutableListOf(errorLoadingMessage) // 显示错误提示作为消息
            } finally {
                _isLoading.value = false // 结束加载，恢复加载状态
            }
        }
    }

    private fun getCurrentDateString(): String { // 私有辅助方法，用于获取当前日期的"yyyy-MM-dd"格式字符串
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // 创建日期格式化对象
        return dateFormat.format(Date()) // 格式化当前日期并返回
    }

    fun sendMessage(userMessageText: String) { // 公开方法，处理用户发送新消息的逻辑
        if (userMessageText.isBlank()) return // 如果消息为空白，则不处理

        val currentKimiApiKey = moonshotApiKey // 获取Kimi API Key
        if (currentKimiApiKey.isBlank()) { // 如果API Key未配置
            _errorMessage.value = "Kimi API Key 未配置或为空。" // 设置错误提示
            val errorApiKeyMessage = ChatMessage( // 创建API Key错误消息
                id = UUID.randomUUID().toString(),
                text = "错误：Kimi API Key 未配置或为空，我暂时无法连接到我的AI大脑。",
                senderType = SenderType.PET, timestamp = System.currentTimeMillis(), date = getCurrentDateString()
            )
            addMessageToLiveDataAndDb(errorApiKeyMessage) // 添加并保存错误消息
            return // 结束流程
        }

        val userMessageObject = ChatMessage( // 创建用户消息对象
            id = UUID.randomUUID().toString(), text = userMessageText, senderType = SenderType.USER,
            timestamp = System.currentTimeMillis(), date = getCurrentDateString()
        )
        addMessageToLiveDataAndDb(userMessageObject) // 添加并保存用户消息

        _isLoading.value = true // 开始网络请求，设置加载状态
        _errorMessage.value = null // 清除旧错误

        val petNameForApiCall = this.currentPetName // 在协程外部获取当前宠物名的副本

        viewModelScope.launch { // 启动协程进行网络请求
            try {
                val historySnapshotWithCurrentUserMessage = _messages.value?.toList() ?: emptyList() // 获取包含当前用户消息的完整消息列表快照

                // <<< 修改点：优化集合操作 (第168行附近)
                val conversationContextForApi = historySnapshotWithCurrentUserMessage
                    .takeLast(10) // 先从原始 ChatMessage 列表取最后10条 (如果少于10条则取全部)
                    .mapNotNull { chatMsg -> // 然后只对这部分消息进行转换
                        when (chatMsg.senderType) {
                            SenderType.USER -> ApiMessage(role = "user", content = chatMsg.text)
                            SenderType.PET -> ApiMessage(role = "assistant", content = chatMsg.text)
                            // 其他 SenderType (如果有) 会在此处被过滤掉，因为 mapNotNull 只保留非null结果
                        }
                    } // 这样 mapNotNull 操作的元素数量最多为10，更高效

                val petReplyText: String = repository.getPetReplyFromNetwork( // 调用仓库获取AI回复
                    userApiKey = currentKimiApiKey,
                    conversationHistory = conversationContextForApi, // 传递构建好的对话历史上下文
                    petName = petNameForApiCall // 使用在协程外部获取的局部变量 petNameForApiCall
                )

                val petMessage: ChatMessage // 声明宠物回复消息对象
                if (!petReplyText.startsWith("API Error:") && !petReplyText.startsWith("API 请求失败:") && !petReplyText.startsWith("网络错误")) { // 如果回复内容不是已知的错误前缀
                    petMessage = ChatMessage( // 创建成功的宠物回复消息
                        id = UUID.randomUUID().toString(), text = petReplyText, senderType = SenderType.PET,
                        timestamp = System.currentTimeMillis(), date = getCurrentDateString()
                    )
                } else { // 如果回复内容是错误字符串
                    _errorMessage.value = petReplyText // 将API返回的错误字符串直接设置为错误消息
                    petMessage = ChatMessage( // 创建表示回复失败或API错误的消息
                        id = UUID.randomUUID().toString(), text = petReplyText, // 直接使用API返回的错误字符串作为消息文本
                        senderType = SenderType.PET, timestamp = System.currentTimeMillis(), date = getCurrentDateString()
                    )
                }
                addMessageToLiveDataAndDb(petMessage) // 添加并保存宠物回复

            } catch (e: Exception) { // 捕获在网络请求或响应处理中发生的其他未知异常
                Log.e("ChatViewModel", "Error sending message: ${e.message}", e) // 记录详细的错误日志
                _errorMessage.value = "发生未知错误: ${e.message}" // 将通用错误信息设置到LiveData
                val errorExceptionMessage = ChatMessage( // 创建一条由宠物发送的表示发生内部错误的提示消息
                    id = UUID.randomUUID().toString(), text = "抱歉，发生了一些内部错误。",
                    senderType = SenderType.PET, timestamp = System.currentTimeMillis(), date = getCurrentDateString()
                )
                addMessageToLiveDataAndDb(errorExceptionMessage) // 添加并保存这条内部错误提示消息
            } finally {
                _isLoading.value = false // 消息发送及回复处理（无论成功或失败）完成后，将加载状态恢复为false
            }
        }
    }

    private fun addMessageToLiveDataAndDb(message: ChatMessage) { // 私有辅助方法，用于将一条消息更新到LiveData并异步保存到数据库
        val currentList = _messages.value ?: mutableListOf() // 获取当前的聊天消息列表；如果LiveData为null，则创建一个新的空可变列表
        currentList.add(message) // 将新消息添加到列表中
        _messages.postValue(currentList) // 使用postValue更新LiveData，确保即使在后台线程调用也是线程安全的

        viewModelScope.launch(Dispatchers.IO) { // 在viewModelScope中启动一个运行在IO分发器上的协程，专门用于执行数据库插入操作
            try { // 使用try-catch块处理数据库操作中可能发生的异常
                repository.insertMessageToDb(message) // 调用数据仓库的方法将消息插入到数据库
            } catch (e: Exception) { // 如果插入过程中发生异常
                Log.e("ChatViewModel", "Error inserting message to DB: ${e.message}", e) // 记录详细的错误日志
            }
        }
    }

    fun clearErrorMessage() { // 公开方法，用于清除当前的错误消息状态
        _errorMessage.value = null // 将错误消息LiveData的值设置为null，通常在UI层显示完错误提示后调用此方法
    }
}