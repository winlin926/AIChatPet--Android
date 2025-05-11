package com.example.aichatpet.ui.chat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
// androidx.lifecycle.Observer // Kotlin可以直接使用lambda
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aichatpet.databinding.ActivityChatBinding
import com.example.aichatpet.ui.chat.adapter.MessageAdapter
import com.example.aichatpet.ui.settings.SettingsActivity
import com.example.aichatpet.viewmodel.ChatViewModel
import com.example.aichatpet.ui.auth.RegisterActivity // <<< 确保导入 RegisterActivity 以使用其常量

class ChatActivity : AppCompatActivity() { // 聊天界面的Activity，负责展示对话消息、处理用户输入以及与ChatViewModel进行交互

    private lateinit var binding: ActivityChatBinding // ViewBinding对象
    private lateinit var messageAdapter: MessageAdapter // RecyclerView的消息适配器
    private val chatViewModel: ChatViewModel by viewModels() // ViewModel实例
    private var chatDate: String? = null // 当前聊天日期，null表示实时聊天

    private val petNameChangedReceiver = object : BroadcastReceiver() { // 广播接收器，监听宠物名变更
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == SettingsActivity.ACTION_PET_NAME_CHANGED) {
                // 使用 RegisterActivity.DEFAULT_PET_NAME 确保默认值一致性
                val newPetName = intent.getStringExtra(SettingsActivity.EXTRA_PET_NAME) ?: RegisterActivity.DEFAULT_PET_NAME
                Log.d("ChatActivity", "BroadcastReceiver - Received new pet name from intent: $newPetName")

                chatViewModel.updatePetName() // 更新ViewModel中的宠物名
                updateToolbarTitle(newPetName) // 更新Toolbar标题

                val currentlyViewedDate = chatDate
                chatViewModel.loadMessages(currentlyViewedDate) // 重新加载消息以触发可能的重介绍逻辑

                Toast.makeText(context, "宠物名称已更新为: $newPetName", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object { // 与此Activity相关的常量
        const val EXTRA_CHAT_DATE = "chat_date_extra" // Intent extra键名，用于传递聊天日期
        // PREFS_NAME, KEY_PET_NAME, DEFAULT_PET_NAME 将直接引用 RegisterActivity 中的常量
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarChat)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        chatDate = intent.getStringExtra(EXTRA_CHAT_DATE)
        val currentPetNameOnCreate = getCurrentPetName() // 从SharedPreferences获取宠物名
        Log.d("ChatActivity", "onCreate - Pet name from SharedPreferences: $currentPetNameOnCreate with PREFS_NAME: ${RegisterActivity.PREFS_NAME} and KEY_PET_NAME: ${RegisterActivity.KEY_PET_NAME}")

        setupRecyclerView()
        observeViewModel()
        updateToolbarTitle(currentPetNameOnCreate) // 更新Toolbar标题

        if (chatDate != null) { // 查看历史记录模式
            chatViewModel.loadMessages(chatDate)
        } else { // 实时聊天模式
            chatViewModel.loadMessages()
        }

        binding.buttonSendMessage.setOnClickListener { // 发送按钮点击事件
            val messageText = binding.editTextChatMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                chatViewModel.sendMessage(messageText)
                binding.editTextChatMessage.setText("")
            } else {
                Toast.makeText(this, "消息不能为空", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(SettingsActivity.ACTION_PET_NAME_CHANGED) // 只接收宠物名更改的广播
        LocalBroadcastManager.getInstance(this).registerReceiver(petNameChangedReceiver, filter) // 注册广播接收器
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(petNameChangedReceiver) // 取消注册广播接收器
    }

    private fun getCurrentPetName(): String { // 从SharedPreferences获取当前宠物名称
        // 使用 RegisterActivity 中定义的统一常量
        val sharedPreferences = getSharedPreferences(RegisterActivity.PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(RegisterActivity.KEY_PET_NAME, RegisterActivity.DEFAULT_PET_NAME)
            ?: RegisterActivity.DEFAULT_PET_NAME // 安全措施，确保返回非null
    }

    private fun updateToolbarTitle(petName: String) { // 更新Toolbar的标题文本
        Log.d("ChatActivity", "updateToolbarTitle CALLED with petName: $petName, chatDate: $chatDate")
        if (chatDate != null) { // 历史记录模式
            supportActionBar?.title = "与${petName}的对话 ($chatDate)"
            Log.d("ChatActivity", "Toolbar title set to: 与${petName}的对话 ($chatDate)")
        } else { // 实时聊天模式
            supportActionBar?.title = "与${petName}对话"
            Log.d("ChatActivity", "Toolbar title set to: 与${petName}对话")
        }
    }

    private fun setupRecyclerView() { // 初始化和配置RecyclerView
        messageAdapter = MessageAdapter() // 创建MessageAdapter实例 (ListAdapter版本，无参构造)
        binding.recyclerViewChatMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true // 新消息总是在底部
            }
            adapter = messageAdapter
        }
    }

    private fun observeViewModel() { // 设置对ViewModel中LiveData的观察
        chatViewModel.messages.observe(this) { messages ->
            messages?.let {
                messageAdapter.submitList(it.toList()) // 使用submitList更新数据，传递副本
            }
            if (messages?.isNotEmpty() == true) {
                binding.recyclerViewChatMessages.smoothScrollToPosition(messages.size - 1) // 滚动到最新消息
            }
        }

        chatViewModel.isLoading.observe(this) { isLoading ->
            binding.progressBarChat.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.buttonSendMessage.isEnabled = !isLoading
            binding.editTextChatMessage.isEnabled = !isLoading
        }

        chatViewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                chatViewModel.clearErrorMessage()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { // 处理Toolbar返回按钮点击
        finish()
        return true
    }
}