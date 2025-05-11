package com.example.aichatpet.ui.auth

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.aichatpet.MainActivity
import com.example.aichatpet.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() { // 登录界面的 Activity 类

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化SharedPreferences，必须在任何视图加载或逻辑判断之前
        sharedPreferences = getSharedPreferences(RegisterActivity.PREFS_NAME, Context.MODE_PRIVATE)

        // 检查是否已登录，如果已登录则直接跳转到主界面
        if (isUserLoggedIn()) {
            navigateToMainActivity()
            return // 必须return，防止执行下面的setContentView等代码，避免不必要的UI加载
        }

        // 如果未登录，则加载登录界面的布局
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonLogin.setOnClickListener {
            val enteredUsername = binding.editTextUsername.text.toString().trim()
            val enteredPassword = binding.editTextPassword.text.toString()

            if (enteredUsername.isBlank() || enteredPassword.isBlank()) {
                Toast.makeText(this, "用户名和密码不能为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val savedUsername = sharedPreferences.getString(RegisterActivity.KEY_USERNAME, null)
            val savedPassword = sharedPreferences.getString(RegisterActivity.KEY_PASSWORD, null)

            if (savedUsername != null && savedPassword != null) {
                if (enteredUsername == savedUsername && enteredPassword == savedPassword) {
                    // 登录成功逻辑
                    saveLoginState(true) // 保存登录状态
                    Toast.makeText(this, "登录成功！", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity() // 跳转到主界面
                } else {
                    Toast.makeText(this, "用户名或密码错误", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "用户不存在或请先注册", Toast.LENGTH_SHORT).show()
            }
        }

        binding.textViewGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun isUserLoggedIn(): Boolean {
        // 从SharedPreferences读取登录状态，默认为false（未登录）
        return sharedPreferences.getBoolean(RegisterActivity.KEY_IS_LOGGED_IN, false)
    }

    private fun saveLoginState(isLoggedIn: Boolean) {
        // 将登录状态保存到SharedPreferences
        sharedPreferences.edit().putBoolean(RegisterActivity.KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        // 设置Flags以清除当前任务栈并将MainActivity作为新任务的根Activity
        // 这样用户按返回键时会退出应用，而不是返回到（已finish的）LoginActivity
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // 关闭当前的LoginActivity
    }
}