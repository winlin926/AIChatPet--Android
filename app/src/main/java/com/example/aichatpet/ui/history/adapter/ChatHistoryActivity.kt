package com.example.aichatpet.ui.history

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aichatpet.databinding.ActivityChatHistoryBinding
import com.example.aichatpet.ui.chat.ChatActivity
import com.example.aichatpet.ui.history.adapter.ChatHistoryAdapter
import com.example.aichatpet.viewmodel.ChatHistoryViewModel
import com.example.aichatpet.ui.auth.RegisterActivity // <<< 新增导入：为了引用统一的SharedPreferences常量

class ChatHistoryActivity : AppCompatActivity() { // 聊天历史记录界面的Activity，用于展示按日期分组的以往对话摘要列表

    private lateinit var binding: ActivityChatHistoryBinding // ViewBinding对象，用于安全地访问此Activity布局 (activity_chat_history.xml) 中的视图控件
    private val viewModel: ChatHistoryViewModel by viewModels() // 通过viewModels委托获取ChatHistoryViewModel的实例，其生命周期与此Activity绑定
    private lateinit var chatHistoryAdapter: ChatHistoryAdapter // RecyclerView的适配器，用于将聊天历史摘要数据绑定到列表项

    // companion object { // <<< 移除此处的 PREFS_NAME, KEY_PET_NAME, DEFAULT_PET_NAME 定义
    // // const val PREFS_NAME = "pet_preferences" // 不再需要，将使用 RegisterActivity.PREFS_NAME
    // // const val KEY_PET_NAME = "pet_name"      // 不再需要，将使用 RegisterActivity.KEY_PET_NAME
    // // const val DEFAULT_PET_NAME = "旺财"      // 不再需要，将使用 RegisterActivity.DEFAULT_PET_NAME
    // }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatHistoryBinding.inflate(layoutInflater) // 初始化ViewBinding，关联activity_chat_history.xml布局文件
        setContentView(binding.root) // 设置Activity的内容视图为ViewBinding的根视图

        setSupportActionBar(binding.toolbarChatHistory) // 将自定义的Toolbar (binding.toolbarChatHistory) 设置为本Activity的ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // 在ActionBar上显示返回(向上导航)按钮，允许用户返回上一级界面
        // supportActionBar?.title = "聊天记录" // Toolbar标题通常在XML布局中通过 app:title 设置，或根据需要动态设置

        setupRecyclerView() // 调用私有方法初始化并配置RecyclerView及其适配器
        observeViewModel()  // 调用私有方法设置对ViewModel中LiveData的观察者，以便在数据变化时自动更新UI
    }

    // 使用从 RegisterActivity 导入的常量来获取宠物名称
    fun getCurrentPetName(): String { // 公共方法 (或可设为private，取决于是否仅内部使用)，用于从SharedPreferences获取当前存储的宠物名称
        val sharedPreferences = getSharedPreferences(RegisterActivity.PREFS_NAME, Context.MODE_PRIVATE) // <<< 修改：使用 RegisterActivity.PREFS_NAME
        return sharedPreferences.getString(RegisterActivity.KEY_PET_NAME, RegisterActivity.DEFAULT_PET_NAME) // <<< 修改：使用 RegisterActivity 的键名和默认名
            ?: RegisterActivity.DEFAULT_PET_NAME // 安全起见，如果 getString 返回 null，则使用默认名
    }

    private fun setupRecyclerView() { // 私有辅助方法，用于初始化和配置RecyclerView
        chatHistoryAdapter = ChatHistoryAdapter( // 创建ChatHistoryAdapter实例
            onItemClicked = { summary -> // 定义列表项被点击时的回调操作
                val intent = Intent(this, ChatActivity::class.java).apply { // 创建一个Intent用于启动ChatActivity
                    putExtra(ChatActivity.EXTRA_CHAT_DATE, summary.date) // 将被点击项的日期 (summary.date) 作为参数传递给ChatActivity，用于加载特定日期的聊天记录
                }
                startActivity(intent) // 执行跳转到ChatActivity的操作
            },
            onItemLongClicked = { summary -> // 定义列表项被长按时的回调操作
                showDeleteConfirmationDialog(summary.date) // 调用方法显示删除确认对话框，并传入要删除的对话日期
            },
            petNameProvider = { getCurrentPetName() } // 将获取当前宠物名称的方法 (getCurrentPetName) 作为lambda表达式传递给适配器，使其能动态获取最新的宠物名
        )
        binding.recyclerViewChatHistory.apply { // 对RecyclerView (binding.recyclerViewChatHistory) 进行配置
            layoutManager = LinearLayoutManager(this@ChatHistoryActivity) // 设置其布局管理器为垂直方向的LinearLayoutManager
            adapter = chatHistoryAdapter // 将创建的chatHistoryAdapter设置给RecyclerView
        }
    }

    private fun observeViewModel() { // 私有辅助方法，用于设置对ViewModel中各个LiveData的观察
        viewModel.dailySummaries.observe(this) { summaries -> // 观察dailySummaries LiveData (每日对话摘要列表) 的变化
            chatHistoryAdapter.submitList(summaries) // 当摘要列表数据更新时，调用适配器(ListAdapter)的submitList方法，它会高效地计算差异并更新列表项
        }

        viewModel.isLoading.observe(this) { isLoading -> // 观察isLoading LiveData (数据加载状态) 的变化
            binding.progressBarChatHistory.visibility = if (isLoading) View.VISIBLE else View.GONE // 根据isLoading状态动态显示或隐藏进度条 (binding.progressBarChatHistory)
        }

        viewModel.isEmpty.observe(this) { isEmpty -> // 观察isEmpty LiveData (聊天历史记录是否为空的状态) 的变化
            if (isEmpty) { // 如果历史记录为空
                binding.recyclerViewChatHistory.visibility = View.GONE // 隐藏RecyclerView
                binding.textViewNoHistory.visibility = View.VISIBLE   // 显示“无历史记录”的提示文本视图 (binding.textViewNoHistory)
            } else { // 如果历史记录不为空
                binding.recyclerViewChatHistory.visibility = View.VISIBLE // 显示RecyclerView
                binding.textViewNoHistory.visibility = View.GONE     // 隐藏“无历史记录”的提示文本视图
            }
        }

        viewModel.errorMessage.observe(this) { errorMessage -> // 观察errorMessage LiveData (错误消息文本) 的变化
            errorMessage?.let { // 如果错误消息不为null (即有错误信息需要显示给用户)
                Toast.makeText(this, it, Toast.LENGTH_LONG).show() // 使用Toast长时间显示错误消息
                viewModel.clearErrorMessage() // 通知ViewModel清除已显示的错误消息，以避免在配置更改等情况下重复提示该错误
            }
        }
    }

    private fun showDeleteConfirmationDialog(dateString: String) { // 私有辅助方法，用于显示一个确认删除特定日期聊天记录的AlertDialog
        val currentPetName = getCurrentPetName() // 获取当前的宠物名称，用于在对话框消息中显示
        AlertDialog.Builder(this) // 创建AlertDialog的构建器实例
            .setTitle("删除确认") // 设置对话框的标题
            .setMessage("确定要删除与${currentPetName}在 $dateString 的所有聊天记录吗？此操作无法撤销。") // 设置对话框的消息内容，包含宠物名、日期，并明确提示操作的不可撤销性
            .setPositiveButton("删除") { _, _ -> // 设置“确定”按钮 (PositiveButton) 及其点击事件监听器
                viewModel.deleteConversationByDate(dateString) // 当用户点击“删除”时，调用ViewModel的方法来执行删除指定日期对话记录的操作
            }
            .setNegativeButton("取消", null) // 设置“取消”按钮 (NegativeButton)，点击时不做任何特殊操作 (null表示直接关闭对话框)
            .show() // 构建并显示AlertDialog
    }

    override fun onSupportNavigateUp(): Boolean { // 当用户点击ActionBar上的返回(向上导航)按钮时调用此方法
        finish() // 关闭当前的ChatHistoryActivity，返回到调用它的前一个Activity或任务栈中的上一个界面
        return true // 返回true表示该导航事件已被成功处理
    }
}