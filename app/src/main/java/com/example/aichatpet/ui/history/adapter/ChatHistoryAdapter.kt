package com.example.aichatpet.ui.history.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.aichatpet.data.model.DailyConversationSummary
import com.example.aichatpet.databinding.ItemChatHistoryBinding // 确保已创建 item_chat_history.xml 对应的 ViewBinding 类

class ChatHistoryAdapter( // RecyclerView的适配器，继承自ListAdapter以利用DiffUtil高效更新列表数据，用于展示聊天历史摘要
    private val onItemClicked: (DailyConversationSummary) -> Unit, // 列表项被点击时的回调函数，接收一个DailyConversationSummary对象作为参数
    private val onItemLongClicked: (DailyConversationSummary) -> Unit, // 列表项被长按时的回调函数，用于触发如删除等操作
    private val petNameProvider: () -> String // 用于动态获取当前宠物名称的函数提供者，使得列表项能显示最新的宠物名
) : ListAdapter<DailyConversationSummary, ChatHistoryAdapter.ChatHistoryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatHistoryViewHolder { // 当RecyclerView需要新的ViewHolder时调用，用于创建列表项的视图和ViewHolder
        val binding = ItemChatHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false) // 使用ViewBinding从item_chat_history.xml布局文件加载并初始化列表项视图
        return ChatHistoryViewHolder(binding) // 创建并返回ChatHistoryViewHolder实例，传入获取到的binding对象
    }

    override fun onBindViewHolder(holder: ChatHistoryViewHolder, position: Int) { // 当RecyclerView需要将数据绑定到特定位置的ViewHolder时调用
        val summary = getItem(position) // 从ListAdapter获取当前位置的数据项 (DailyConversationSummary对象)
        holder.bind(summary, petNameProvider()) // 调用ViewHolder的bind方法，传递当前数据项和通过petNameProvider()获取的最新宠物名称
    }

    inner class ChatHistoryViewHolder(private val binding: ItemChatHistoryBinding) : RecyclerView.ViewHolder(binding.root) { // ViewHolder类，持有对item_chat_history.xml布局中各个视图控件的引用，并处理项的点击/长按事件
        init { // ViewHolder的初始化块，通常在此处为列表项视图设置点击监听器
            binding.root.setOnClickListener { // 为整个列表项的根视图 (binding.root) 设置点击监听器
                val position = adapterPosition // 获取当前被点击项在适配器中的准确位置 (使用adapterPosition比layoutPosition更安全)
                if (position != RecyclerView.NO_POSITION) { // 检查获取到的位置是否有效 (不是RecyclerView.NO_POSITION，表示项未被移除或处于无效状态)
                    onItemClicked(getItem(position)) // 如果位置有效，则调用通过构造函数传入的onItemClicked回调函数，并传递当前项的数据
                }
            }
            binding.root.setOnLongClickListener { // 为整个列表项的根视图设置长按监听器
                val position = adapterPosition // 获取当前被长按项在适配器中的准确位置
                if (position != RecyclerView.NO_POSITION) { // 检查位置是否有效
                    onItemLongClicked(getItem(position)) // 如果位置有效，调用onItemLongClicked回调函数，并传递当前项的数据
                    true // 返回true表示长按事件已被成功消费，后续的短按事件(如果存在)将不会被触发
                } else {
                    false // 如果位置无效，返回false表示未消费此事件
                }
            }
        }

        fun bind(summary: DailyConversationSummary, petName: String) { // ViewHolder的bind方法，负责将DailyConversationSummary数据和宠物名称填充到列表项的对应视图中
            binding.textViewHistoryDate.text = "与${petName}的对话 (${summary.date})" // 设置日期显示文本，格式为 "与[宠物名]的对话 ([日期])"
            binding.textViewHistoryLastMessage.text = summary.lastMessageSnippet // 设置最后一条消息的摘要文本
            binding.textViewHistoryMessageCount.text = "${summary.messageCount} 条消息" // 设置该日对话的消息总数文本，例如 "15 条消息"
            // 如果ItemChatHistoryBinding中包含用于显示宠物头像的ImageView，可以在此设置头像
            // 例如: binding.imageViewPetAvatar.setImageResource(R.drawable.ic_default_pet_avatar) // 假设有一个默认宠物头像资源
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<DailyConversationSummary>() { // DiffUtil.ItemCallback的实现，供ListAdapter用于在后台线程高效计算数据列表的差异
        override fun areItemsTheSame(oldItem: DailyConversationSummary, newItem: DailyConversationSummary): Boolean { // 判断两个数据项是否代表同一个现实世界的对象 (通常基于唯一ID比较)
            return oldItem.date == newItem.date // 在此应用中，我们假设日期 (date) 是每日对话摘要的唯一标识符
        }

        override fun areContentsTheSame(oldItem: DailyConversationSummary, newItem: DailyConversationSummary): Boolean { // 判断两个代表相同对象的数据项的内容是否发生了实际变化
            return oldItem == newItem // 由于DailyConversationSummary是一个data class，'=='操作符会自动比较其所有字段的内容是否相同
        }
    }
}