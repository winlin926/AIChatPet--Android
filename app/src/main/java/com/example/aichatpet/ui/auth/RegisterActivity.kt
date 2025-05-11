package com.example.aichatpet.ui.auth

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.aichatpet.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() { // 注册界面的 Activity 类，负责处理用户注册新账户的逻辑

    private lateinit var binding: ActivityRegisterBinding // ViewBinding 对象，用于安全地访问此Activity布局中的视图控件
    private lateinit var sharedPreferences: SharedPreferences // SharedPreferences 对象，用于存储新注册用户的凭据

    companion object { // 存放 SharedPreferences 相关的常量，作为整个应用的权威定义源
        const val PREFS_NAME = "UserPrefs" // <<< 应用统一使用的 SharedPreferences 文件名
        const val KEY_USERNAME = "username" // 定义存储用户名的键名
        const val KEY_PASSWORD = "password" // 定义存储密码的键名
        const val KEY_EMAIL = "email"       // (可选) 定义存储邮箱的键名
        const val KEY_PET_NAME = "pet_name" // <<< 应用统一使用的宠物名键名
        const val DEFAULT_PET_NAME = "旺财" // <<< 应用统一使用的默认宠物名
        const val KEY_IS_LOGGED_IN = "is_logged_in" // <<< 新增：用于保存登录状态的键名
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater) // 初始化ViewBinding，关联activity_register.xml布局文件
        setContentView(binding.root) // 设置Activity的内容视图为ViewBinding的根视图

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) // 初始化SharedPreferences实例

        binding.buttonRegister.setOnClickListener { // 设置注册按钮的点击事件监听器
            val username = binding.editTextRegisterUsername.text.toString().trim()
            val email = binding.editTextRegisterEmail.text.toString().trim()
            val password = binding.editTextRegisterPassword.text.toString()
            val confirmPassword = binding.editTextConfirmPassword.text.toString()

            if (username.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                Toast.makeText(this, "所有字段均为必填项", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "请输入有效的邮箱地址", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "密码长度至少为6位", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val editor = sharedPreferences.edit()
            editor.putString(KEY_USERNAME, username)
            editor.putString(KEY_PASSWORD, password) // 实际应用中密码应加密存储
            editor.putString(KEY_EMAIL, email)
            // 可以在注册时设置默认宠物名和初始登录状态（虽然登录成功后会再次设置）
            // editor.putString(KEY_PET_NAME, DEFAULT_PET_NAME)
            // editor.putBoolean(KEY_IS_LOGGED_IN, false) // 初始应为未登录，或在登录成功时明确设置
            editor.apply()

            Toast.makeText(this, "注册成功！请返回登录。", Toast.LENGTH_LONG).show()
            // 通常注册成功后不会自动登录，而是让用户去登录页面登录
            // 如果需要直接登录，则需要在这里保存登录状态并跳转到MainActivity
            finish() // 关闭当前的RegisterActivity，返回到LoginActivity
        }

        binding.textViewGoToLogin.setOnClickListener {
            finish() // 关闭RegisterActivity，返回到调用它的LoginActivity
        }
    }
}