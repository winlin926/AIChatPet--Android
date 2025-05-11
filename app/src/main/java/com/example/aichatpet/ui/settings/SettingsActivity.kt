package com.example.aichatpet.ui.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.aichatpet.MyApplication
import com.example.aichatpet.databinding.ActivitySettingsBinding
import com.example.aichatpet.ui.auth.LoginActivity
import com.example.aichatpet.ui.auth.RegisterActivity // 确保导入RegisterActivity以访问其常量
import com.example.aichatpet.viewmodel.ClearHistoryStatus
import com.example.aichatpet.viewmodel.SettingsViewModel

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sharedPreferences: SharedPreferences

    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModel.SettingsViewModelFactory(
            application as MyApplication,
            (application as MyApplication).database.chatMessageDao()
        )
    }

    private val prefsFileName = RegisterActivity.PREFS_NAME
    private val keyForPetName = RegisterActivity.KEY_PET_NAME
    private val defaultPetNameValue = RegisterActivity.DEFAULT_PET_NAME

    companion object {
        const val ACTION_PET_NAME_CHANGED = "com.example.aichatpet.PET_NAME_CHANGED"
        const val EXTRA_PET_NAME = RegisterActivity.KEY_PET_NAME
        const val KEY_PET_NAME_JUST_CHANGED = "pet_name_just_changed_flag"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences(prefsFileName, Context.MODE_PRIVATE)

        setSupportActionBar(binding.toolbarSettings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "设置"

        loadCurrentPetName()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.layoutPetName.setOnClickListener { showEditPetNameDialog() }
        binding.layoutClearHistory.setOnClickListener { showClearHistoryConfirmationDialog() }
        binding.layoutApiSettings.setOnClickListener {
            // startActivity(Intent(this, ApiConfigActivity::class.java)) // 假设ApiConfigActivity存在
            Toast.makeText(this, "API配置页面待实现", Toast.LENGTH_SHORT).show()
        }
        binding.layoutAboutApp.setOnClickListener { showAboutAppDialog() }
        binding.buttonLogout.setOnClickListener { showLogoutConfirmationDialog() }
    }

    private fun loadCurrentPetName() {
        val currentPetName = sharedPreferences.getString(keyForPetName, defaultPetNameValue)
        binding.textViewCurrentPetName.text = currentPetName
        Log.d("SettingsActivity", "Loaded pet name: $currentPetName")
    }

    private fun showEditPetNameDialog() {
        val editText = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(sharedPreferences.getString(keyForPetName, defaultPetNameValue))
            hint = "请输入新的宠物名"
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20)
            addView(editText)
        }
        AlertDialog.Builder(this)
            .setTitle("修改宠物名称")
            .setView(layout)
            .setPositiveButton("确定") { _, _ ->
                val newPetName = editText.text.toString().trim()
                if (newPetName.isNotEmpty()) {
                    val oldPetName = sharedPreferences.getString(keyForPetName, defaultPetNameValue)
                    if (newPetName != oldPetName) {
                        sharedPreferences.edit()
                            .putString(keyForPetName, newPetName)
                            .putBoolean(KEY_PET_NAME_JUST_CHANGED, true)
                            .apply()
                        loadCurrentPetName()
                        sendPetNameChangedBroadcast(newPetName)
                        Toast.makeText(this, "宠物名称已更新为：$newPetName", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "宠物名称未变更", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "宠物名称不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun sendPetNameChangedBroadcast(newPetName: String) {
        val intent = Intent(ACTION_PET_NAME_CHANGED).apply {
            putExtra(EXTRA_PET_NAME, newPetName)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        Log.d("SettingsActivity", "Pet name change broadcast sent: $newPetName")
    }

    private fun showClearHistoryConfirmationDialog() {
        val currentPetNameDisplay = binding.textViewCurrentPetName.text.toString()
        AlertDialog.Builder(this)
            .setTitle("清除确认")
            .setMessage("确定要清除所有与 $currentPetNameDisplay 的聊天记录吗？此操作无法撤销。")
            .setPositiveButton("全部清除") { _, _ ->
                settingsViewModel.clearAllChatHistory()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAboutAppDialog() {
        val poem = """
            三生有幸遇卿颜，
            林花绽放映娇妍。
            安闲漫步时光里，
            奇玉玲珑心相连。
        """.trimIndent()
        AlertDialog.Builder(this)
            .setTitle("关于 AI宠物伴侣")
            .setMessage(poem)
            .setPositiveButton("好的", null)
            .show()
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("退出登录")
            .setMessage("确定要退出当前账号吗？")
            .setPositiveButton("退出") { _, _ ->
                val userPrefsEditor = this.sharedPreferences.edit()
                // 清除用户凭据
                userPrefsEditor.remove(RegisterActivity.KEY_USERNAME)
                userPrefsEditor.remove(RegisterActivity.KEY_PASSWORD)
                userPrefsEditor.remove(RegisterActivity.KEY_EMAIL)
                // 清除登录状态标记
                userPrefsEditor.putBoolean(RegisterActivity.KEY_IS_LOGGED_IN, false) // <<< 重要修改
                // 可选：是否清除宠物名和改名标记
                // userPrefsEditor.remove(keyForPetName)
                // userPrefsEditor.remove(KEY_PET_NAME_JUST_CHANGED)
                userPrefsEditor.apply()

                Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finishAffinity() // 关闭当前任务中的所有Activity，确保回到登录页时栈是干净的
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun observeViewModel() {
        settingsViewModel.clearHistoryStatus.observe(this) { status ->
            when (status) {
                ClearHistoryStatus.LOADING -> {
                    binding.layoutClearHistory.isEnabled = false
                    binding.buttonLogout.isEnabled = false
                    Toast.makeText(this, "正在清除聊天记录...", Toast.LENGTH_SHORT).show()
                }
                ClearHistoryStatus.SUCCESS -> {
                    Toast.makeText(this, "聊天记录已清除！", Toast.LENGTH_LONG).show()
                    settingsViewModel.onClearHistoryStatusHandled() // 重置状态
                    binding.layoutClearHistory.isEnabled = true
                    binding.buttonLogout.isEnabled = true
                }
                ClearHistoryStatus.FAILURE -> {
                    // 错误消息应通过 errorMessage LiveData 显示
                    settingsViewModel.onClearHistoryStatusHandled() // 重置状态
                    binding.layoutClearHistory.isEnabled = true
                    binding.buttonLogout.isEnabled = true
                }
                ClearHistoryStatus.IDLE, null -> {
                    binding.layoutClearHistory.isEnabled = true
                    binding.buttonLogout.isEnabled = true
                }
            }
        }

        settingsViewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                settingsViewModel.clearErrorMessage() // 确保错误消息只显示一次
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}