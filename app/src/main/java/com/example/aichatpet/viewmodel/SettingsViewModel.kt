package com.example.aichatpet.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aichatpet.MyApplication
import com.example.aichatpet.data.local.ChatMessageDao
import com.example.aichatpet.data.repository.ChatRepository
import kotlinx.coroutines.launch

class SettingsViewModel( // 设置界面的ViewModel，继承自AndroidViewModel
    application: Application, // AndroidViewModel需要Application参数，方便访问应用级资源或上下文
    private val chatRepository: ChatRepository // 通过构造函数注入ChatRepository，用于执行数据操作，如此处的清除聊天记录
) : AndroidViewModel(application) {

    private val _clearHistoryStatus = MutableLiveData<ClearHistoryStatus>() // 私有的、可变的LiveData，用于跟踪清除聊天记录操作的当前状态
    val clearHistoryStatus: LiveData<ClearHistoryStatus> = _clearHistoryStatus // 公开的、不可变的LiveData，UI层通过它观察清除操作的状态变化

    private val _errorMessage = MutableLiveData<String?>() // 私有的、可变的LiveData，用于存储操作过程中可能发生的错误消息文本
    val errorMessage: LiveData<String?> = _errorMessage // 公开的LiveData，UI层通过它观察错误消息，以便向用户显示提示

    fun clearAllChatHistory() { // 公开方法，用于触发清除所有聊天记录的操作
        viewModelScope.launch { // 在viewModelScope中启动一个协程执行清除操作，确保在ViewModel生命周期内管理
            _clearHistoryStatus.postValue(ClearHistoryStatus.LOADING) // 在开始清除前，将状态更新为LOADING (使用postValue是因可能在后台线程更新)
            try { // 使用try-catch块处理清除过程中可能发生的异常
                chatRepository.deleteAllMessagesFromDb() // 调用ChatRepository的方法删除数据库中的所有聊天消息
                Log.d("SettingsViewModel", "All chat history cleared successfully.") // 清除成功后，记录日志
                _clearHistoryStatus.postValue(ClearHistoryStatus.SUCCESS) // 将状态更新为SUCCESS
            } catch (e: Exception) { // 如果清除过程中发生任何异常
                Log.e("SettingsViewModel", "Error clearing chat history: ${e.message}", e) // 记录详细的错误日志
                _errorMessage.postValue("清除聊天记录失败: ${e.localizedMessage}") // 将错误消息设置到LiveData (使用localizedMessage获取用户友好的错误信息)
                _clearHistoryStatus.postValue(ClearHistoryStatus.FAILURE) // 将状态更新为FAILURE
            }
        }
    }

    fun onClearHistoryStatusHandled() { // 公开方法，用于在UI层处理完清除操作的状态（如显示Toast）后，重置状态
        _clearHistoryStatus.value = ClearHistoryStatus.IDLE // 将清除状态重置为IDLE (空闲/已处理)，避免因配置更改等导致重复响应旧状态
    }

    fun clearErrorMessage() { // 公开方法，用于清除当前的错误消息状态
        _errorMessage.value = null // 将错误消息LiveData的值设置为null，通常在UI层显示完错误提示后调用此方法
    }

    class SettingsViewModelFactory( // 自定义的ViewModelProvider.Factory，用于创建带有构造函数参数的SettingsViewModel实例
        private val application: Application, // 需要Application实例来创建AndroidViewModel
        private val chatMessageDao: ChatMessageDao // 需要ChatMessageDao实例来创建ChatRepository，进而注入到SettingsViewModel
    ) : ViewModelProvider.Factory { // 实现ViewModelProvider.Factory接口
        override fun <T : ViewModel> create(modelClass: Class<T>): T { // 重写create方法，负责创建ViewModel实例
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) { // 检查请求的ViewModel类型是否为SettingsViewModel或其子类
                @Suppress("UNCHECKED_CAST") // 抑制未经检查的类型转换警告，因为我们已经通过isAssignableFrom进行了类型检查
                return SettingsViewModel(application, ChatRepository(chatMessageDao)) as T // 创建ChatRepository实例并用它和application创建SettingsViewModel实例，然后转换为请求的类型T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}") // 如果请求的类型不是SettingsViewModel，则抛出异常
        }
    }
}

enum class ClearHistoryStatus { // 枚举类，用于清晰地表示清除聊天记录操作的各种可能状态
    IDLE,    // 空闲状态：操作未开始，或已成功/失败并被UI处理完毕后的状态
    LOADING, // 加载状态：清除操作正在进行中
    SUCCESS, // 成功状态：清除操作已成功完成
    FAILURE  // 失败状态：清除操作因发生错误而失败
}