package com.example.aichatpet

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.aichatpet.databinding.ActivityMainBinding
import com.example.aichatpet.ui.chat.ChatActivity
import com.example.aichatpet.ui.history.ChatHistoryActivity
import com.example.aichatpet.ui.image.ImageAnalysisActivity
import com.example.aichatpet.ui.settings.SettingsActivity

class MainActivity : AppCompatActivity() { // 应用的主界面Activity，作为导航中心，提供到各个主要功能的入口

    private lateinit var binding: ActivityMainBinding // ViewBinding对象，用于安全地访问此Activity布局 (activity_main.xml) 中的视图控件

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater) // 初始化ViewBinding，关联activity_main.xml布局文件
        setContentView(binding.root) // 设置Activity的内容视图为ViewBinding的根视图

        binding.buttonGoToChat.setOnClickListener { // 为“进入聊天”按钮 (binding.buttonGoToChat) 设置点击事件监听器
            val intent = Intent(this, ChatActivity::class.java) // 创建一个Intent，用于从当前MainActivity跳转到ChatActivity
            startActivity(intent) // 执行跳转操作
        }

        binding.buttonGoToImageUpload.setOnClickListener { // 为“进入图像分析”按钮 (binding.buttonGoToImageUpload) 设置点击事件监听器
            val intent = Intent(this, ImageAnalysisActivity::class.java) // 创建一个Intent，用于跳转到ImageAnalysisActivity
            startActivity(intent) // 执行跳转操作
        }

        binding.buttonGoToHistory.setOnClickListener { // 为“查看对话历史”按钮 (binding.buttonGoToHistory) 设置点击事件监听器
            val intent = Intent(this, ChatHistoryActivity::class.java) // 创建一个Intent，用于跳转到ChatHistoryActivity
            startActivity(intent) // 执行跳转操作
        }

        binding.buttonGoToSettings.setOnClickListener { // 为“进入设置”按钮 (binding.buttonGoToSettings) 设置点击事件监听器
            val intent = Intent(this, SettingsActivity::class.java) // 创建一个Intent，用于跳转到SettingsActivity
            startActivity(intent) // 执行跳转操作
        }
    }
}