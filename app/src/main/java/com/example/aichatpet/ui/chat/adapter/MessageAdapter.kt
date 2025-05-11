package com.example.aichatpet.ui.chat.adapter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter // <<< 修改：继承自 ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.aichatpet.R
import com.example.aichatpet.data.model.ChatMessage
import com.example.aichatpet.data.model.SenderType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 修改：类声明，继承自 ListAdapter，并传入 ChatMessageDiffCallback
class MessageAdapter : ListAdapter<ChatMessage, MessageAdapter.MessageViewHolder>(ChatMessageDiffCallback()) { // RecyclerView的适配器，使用ListAdapter以高效处理消息列表的更新

    companion object { // 定义用于区分不同消息视图类型的常量，虽然目前布局相同，但类型可用于未来扩展或特定逻辑处理
        private const val VIEW_TYPE_USER_MESSAGE = 1 // 代表用户发送的消息的视图类型
        private const val VIEW_TYPE_PET_MESSAGE = 2  // 代表宠物发送的消息的视图类型
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) { // ViewHolder类，用于缓存列表项视图中的子控件引用，以提高滚动性能
        val messageText: TextView = itemView.findViewById(R.id.textViewMessageText) // 用于显示消息文本内容的TextView
        val messageSender: TextView = itemView.findViewById(R.id.textViewMessageSender) // (可选) 用于显示消息发送者名称的TextView (例如 "我", "AI宠物")
        val messageTimestamp: TextView = itemView.findViewById(R.id.textViewMessageTimestamp) // 用于显示消息发送时间的TextView
        val messageContainer: LinearLayout = itemView as LinearLayout // 列表项的根布局容器 (LinearLayout)，用于动态调整消息气泡的对齐方式
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder { // 当RecyclerView需要新的ViewHolder时调用，用于创建并初始化ViewHolder
        val view = LayoutInflater.from(parent.context) // 从父容器的上下文中获取LayoutInflater实例
            .inflate(R.layout.item_chat_message, parent, false) // 加载列表项的布局文件 (item_chat_message.xml)，并将其附加到父视图组 (但不立即附加到根)
        return MessageViewHolder(view) // 创建并返回一个新的MessageViewHolder实例，持有刚创建的列表项视图
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) { // 当RecyclerView需要将数据绑定到特定位置的ViewHolder时调用
        val message = getItem(position) // <<< 修改：使用 ListAdapter 的 getItem(position) 方法获取数据项

        holder.messageText.text = message.text // 将消息对象的文本内容设置到ViewHolder中的messageText TextView上

        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault()) // 创建一个日期时间格式化对象，用于将时间戳转换为"小时:分钟"格式的字符串
        holder.messageTimestamp.text = sdf.format(Date(message.timestamp)) // 将消息的时间戳 (Long类型) 转换为Date对象，再格式化成字符串并设置到messageTimestamp TextView上

        // 根据发送者调整样式和对齐 (注意：如果使用 VIEW_TYPE_USER_MESSAGE 和 VIEW_TYPE_PET_MESSAGE 来加载不同的布局，
        // 很多样式相关的代码可以直接在不同的 item 布局XML中定义，从而简化 onBindViewHolder)
        if (message.senderType == SenderType.USER) { // 判断消息的发送者类型是否为用户
            holder.messageSender.text = "我" // 如果是用户发送的，发送者名称显示为"我" (或实际用户名)
            holder.messageContainer.gravity = Gravity.END // 将用户消息容器的内容物 (消息气泡) 整体靠右对齐
            holder.messageText.background = ContextCompat.getDrawable(holder.itemView.context, R.drawable.bg_chat_bubble_user) // 设置用户消息气泡的背景
            holder.messageText.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.white)) // 设置用户消息的文本颜色
        } else { // SenderType.PET // 如果消息的发送者类型是宠物
            // 在实际应用中，应该从SharedPreferences或ViewModel获取最新的宠物名并显示在这里
            // holder.messageSender.text = "AI宠物" // 示例：发送者名称显示为"AI宠物" (应替换为动态获取的宠物名)
            // 为了简化MessageAdapter，通常会将宠物名作为ChatMessage的一部分，或通过其他方式传递给Adapter
            // 暂时保持 "AI宠物"，如果ChatMessage本身不包含发送者显示名，这部分逻辑可能需要调整或在ViewModel中预处理数据
            val petName = "AI宠物" // 临时硬编码，理想情况下从 SharedPreferences 或 ViewModel 传入/获取
            holder.messageSender.text = petName // 显示宠物名
            holder.messageContainer.gravity = Gravity.START // 将宠物消息容器的内容物整体靠左对齐
            holder.messageText.background = ContextCompat.getDrawable(holder.itemView.context, R.drawable.bg_chat_bubble_pet) // 设置宠物消息气泡的背景
            holder.messageText.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.black)) // 设置宠物消息的文本颜色
        }
    }

    // getItemCount() 方法不再需要手动覆盖，ListAdapter 会自动处理

    // updateMessages() 方法被 ListAdapter 的 submitList() 方法替代，
    // 你将从 ChatActivity/ChatViewModel 中调用 messageAdapter.submitList(newListOfMessages)

    override fun getItemViewType(position: Int): Int { // 根据指定位置的消息数据返回其对应的视图类型
        val message = getItem(position) // <<< 修改：使用 ListAdapter 的 getItem(position) 方法获取数据项
        return if (message.senderType == SenderType.USER) { // 判断消息发送者
            VIEW_TYPE_USER_MESSAGE // 如果是用户，返回用户消息的视图类型常量
        } else {
            VIEW_TYPE_PET_MESSAGE // 如果是宠物，返回宠物消息的视图类型常量
        }
    }
}

// DiffUtil.ItemCallback 的实现，用于比较 ChatMessage 对象
class ChatMessageDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
    override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean { // 判断两个对象是否代表同一个Item（通常基于唯一ID）
        return oldItem.id == newItem.id // 假设ChatMessage有一个唯一的 'id' 字段用于区分不同的消息
    }

    override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean { // 判断同一个Item的内容是否发生了变化
        return oldItem == newItem // 由于ChatMessage是data class，'=='操作符会自动比较其所有字段的内容是否相同
    }
}