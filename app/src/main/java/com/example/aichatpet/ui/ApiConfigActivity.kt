package com.example.aichatpet.ui.settings // 确保包名正确

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.aichatpet.databinding.ActivityApiConfigBinding // 确保Binding类名与你的布局文件名匹配

class ApiConfigActivity : AppCompatActivity() { // API配置界面的Activity，允许用户输入和保存Kimi API的密钥和端点信息

    private lateinit var binding: ActivityApiConfigBinding // ViewBinding对象，用于安全地访问此Activity布局中的视图控件
    private lateinit var prefs: SharedPreferences // SharedPreferences对象，用于存储和读取API相关的配置信息

    companion object { // 存放与API配置相关的常量，如SharedPreferences文件名、键名以及默认值
        const val PREFS_API_CONFIG = "ApiConfigPrefs" // API配置专用的SharedPreferences文件名
        // 百度相关的Key已移除
        const val KEY_KIMI_API_KEY = "kimi_api_key" // 定义存储Kimi (Moonshot) API Key的键名
        const val KEY_KIMI_API_ENDPOINT = "kimi_api_endpoint" // 定义存储Kimi (Moonshot) API端点URL的键名

        // 注意：真实的API Key不应硬编码在此处，此处默认值主要用于显示或作为用户未配置时的初始提示
        // 百度相关的默认值已移除
        const val DEFAULT_KIMI_API_ENDPOINT = "https://api.moonshot.cn/v1" // Kimi (Moonshot) API端点的默认URL
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApiConfigBinding.inflate(layoutInflater) // 初始化ViewBinding，关联activity_api_config.xml布局文件
        setContentView(binding.root) // 设置Activity的内容视图为ViewBinding的根视图

        prefs = getSharedPreferences(PREFS_API_CONFIG, Context.MODE_PRIVATE) // 初始化SharedPreferences实例，使用上面定义的PREFS_API_CONFIG作为文件名

        setSupportActionBar(binding.toolbarApiConfig) // 将自定义的Toolbar (binding.toolbarApiConfig) 设置为本Activity的ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // 在ActionBar上显示返回(向上导航)按钮
        // Toolbar的标题已在XML布局中通过 app:title="API 配置" 设置

        loadConfigurations() // 调用方法从SharedPreferences加载已保存的API配置信息并显示到对应的EditText输入框中

        binding.buttonSaveApiConfig.setOnClickListener { // 为“保存配置”按钮 (binding.buttonSaveApiConfig) 设置点击事件监听器
            saveConfigurations() // 当用户点击按钮时，调用saveConfigurations方法将当前输入框中的内容保存到SharedPreferences
        }
    }

    private fun loadConfigurations() { // 从SharedPreferences加载API配置并填充到UI输入框
        // 百度相关的加载代码已移除

        binding.editTextKimiApiKey.setText(prefs.getString(KEY_KIMI_API_KEY, "")) // 加载Kimi API Key；如果SharedPreferences中未存储，则显示空字符串
        binding.editTextKimiApiEndpoint.setText(prefs.getString(KEY_KIMI_API_ENDPOINT, DEFAULT_KIMI_API_ENDPOINT)) // 加载Kimi API端点URL；如果未存储，则显示预定义的默认值
    }

    private fun saveConfigurations() { // 将用户在UI输入框中修改的API配置信息保存到SharedPreferences
        val editor = prefs.edit() // 获取SharedPreferences的编辑器实例，用于写入数据
        // 百度相关的保存代码已移除

        editor.putString(KEY_KIMI_API_KEY, binding.editTextKimiApiKey.text.toString().trim()) // 保存用户输入的Kimi API Key (去除首尾多余空格)
        editor.putString(KEY_KIMI_API_ENDPOINT, binding.editTextKimiApiEndpoint.text.toString().trim()) // 保存用户输入的Kimi API端点URL (去除首尾空格)

        editor.apply() // 异步提交所有更改到SharedPreferences文件
        Toast.makeText(this, "API 配置已保存", Toast.LENGTH_SHORT).show() // 通过Toast向用户显示配置已成功保存的提示
        // finish() // 可选：如果希望保存后自动关闭此配置页面，可以取消此行注释
    }

    override fun onSupportNavigateUp(): Boolean { // 当用户点击ActionBar上的返回(向上导航)按钮时调用此方法
        finish() // 关闭当前的ApiConfigActivity，返回到调用它的前一个Activity (通常是SettingsActivity)
        return true // 返回true表示该导航事件已被成功处理
    }
}