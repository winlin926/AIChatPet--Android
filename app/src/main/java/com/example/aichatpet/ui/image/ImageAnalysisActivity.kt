package com.example.aichatpet.ui.image

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import coil.load // 导入 Coil 图片加载库
import com.example.aichatpet.databinding.ActivityImageAnalysisBinding
import com.example.aichatpet.ui.auth.RegisterActivity // <<< 新增导入：为了引用统一的SharedPreferences常量
import com.example.aichatpet.ui.settings.SettingsActivity
import com.example.aichatpet.viewmodel.ImageAnalysisViewModel

class ImageAnalysisActivity : AppCompatActivity() { // 图像分析界面的Activity，负责用户选择图片、调用ViewModel进行分析并展示结果

    private lateinit var binding: ActivityImageAnalysisBinding // ViewBinding对象
    private val viewModel: ImageAnalysisViewModel by viewModels() // ViewModel实例

    companion object { // 与此Activity相关的常量
        private const val TAG = "ImageAnalysisActivity"   // 日志TAG
        // PREFS_NAME, KEY_PET_NAME, DEFAULT_PET_NAME 将直接引用 RegisterActivity 中的常量
    }

    private val petNameChangedReceiver = object : BroadcastReceiver() { // 广播接收器，监听宠物名变更
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == SettingsActivity.ACTION_PET_NAME_CHANGED) {
                val newPetName = intent.getStringExtra(SettingsActivity.EXTRA_PET_NAME) ?: RegisterActivity.DEFAULT_PET_NAME // 使用统一默认名
                Log.d(TAG, "BroadcastReceiver - Received new pet name: $newPetName") // 日志

                viewModel.updatePetName() // 更新ViewModel中的宠物名
                updateUIWithPetName(newPetName) // 更新Toolbar标题等UI
            }
        }
    }

    private val pickMediaLauncher = // Activity结果启动器，处理现代照片选择器返回的结果
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) { // 如果用户成功选择图片
                Log.d(TAG, "图片已选择: $uri")
                viewModel.setSelectedImageUri(uri) // 通知ViewModel更新URI
            } else { // 用户取消选择
                Log.d(TAG, "没有选择图片")
                // viewModel.setSelectedImageUri(null) // 可选：如果希望取消时清空预览
            }
        }

    private val requestStoragePermissionLauncher = // Activity结果启动器，处理存储权限请求的结果
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) { // 权限被授予
                Log.d(TAG, "存储权限已授予")
                launchImagePicker() // 启动图片选择器
            } else { // 权限被拒绝
                Log.w(TAG, "存储权限被拒绝")
                Toast.makeText(this, "你需要授予存储权限才能选择本地图片哦！", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageAnalysisBinding.inflate(layoutInflater) // 初始化ViewBinding
        setContentView(binding.root) // 设置内容视图

        setSupportActionBar(binding.toolbarImageAnalysis) // 设置Toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // 显示返回按钮

        val currentPetNameOnCreate = getCurrentPetName() // 从SharedPreferences获取宠物名
        Log.d(TAG, "onCreate - Pet name from SharedPreferences: $currentPetNameOnCreate with PREFS_NAME: ${RegisterActivity.PREFS_NAME} and KEY_PET_NAME: ${RegisterActivity.KEY_PET_NAME}") // 日志
        updateUIWithPetName(currentPetNameOnCreate) // 更新UI

        binding.buttonSelectImage.setOnClickListener { // “选择图片”按钮点击事件
            Log.d(TAG, "选择图片按钮被点击")
            checkAndLaunchImagePicker() // 检查权限并启动图片选择器
        }

        binding.buttonAnalyzeImage.setOnClickListener { // “开始分析”按钮点击事件
            Log.d(TAG, "开始分析按钮被点击")
            viewModel.analyzeSelectedImage() // 调用ViewModel分析图片
        }

        observeViewModelChanges() // 观察ViewModel中的LiveData
    }

    override fun onStart() { // Activity可见时
        super.onStart()
        val filter = IntentFilter(SettingsActivity.ACTION_PET_NAME_CHANGED) // 创建IntentFilter
        LocalBroadcastManager.getInstance(this).registerReceiver(petNameChangedReceiver, filter) // 注册广播接收器
    }

    override fun onStop() { // Activity不再可见时
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(petNameChangedReceiver) // 取消注册
    }

    private fun getCurrentPetName(): String { // 从SharedPreferences读取宠物名称
        val sharedPreferences = getSharedPreferences(RegisterActivity.PREFS_NAME, Context.MODE_PRIVATE) // 使用统一文件名
        return sharedPreferences.getString(RegisterActivity.KEY_PET_NAME, RegisterActivity.DEFAULT_PET_NAME) // 使用统一键名和默认名
            ?: RegisterActivity.DEFAULT_PET_NAME
    }

    private fun updateUIWithPetName(petName: String) { // 更新UI上的宠物名相关元素 (主要是Toolbar标题)
        Log.d(TAG, "updateUIWithPetName CALLED with petName: $petName")
        supportActionBar?.title = "${petName}的看图分析" // 设置Toolbar标题
        Log.d(TAG, "Toolbar title set to: ${supportActionBar?.title}")
        // 如果布局中有其他显示宠物名的TextView，也在此更新，例如:
        // binding.textViewPetNameOnScreen.text = petName
        binding.textViewAnalysisLabel.text = "${petName}的看法：" // 修改：更新统一结果区的标签文本
    }

    private fun checkAndLaunchImagePicker() { // 检查权限并启动图片选择器
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) { // Android 9及以下需显式权限
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> launchImagePicker()
                shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> showPermissionRationaleDialog(Manifest.permission.READ_EXTERNAL_STORAGE)
                else -> requestStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else { // Android 10及以上，直接使用PickVisualMedia
            launchImagePicker()
        }
    }

    private fun launchImagePicker() { // 启动图片选择器
        Log.d(TAG, "启动图片选择器...")
        try {
            pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) // 限定只选图片
        } catch (e: Exception) {
            Log.e(TAG, "启动图片选择器失败", e)
            Toast.makeText(this, "无法打开图片选择器: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showPermissionRationaleDialog(permission: String) { // 显示权限说明对话框
        AlertDialog.Builder(this)
            .setTitle("需要权限")
            .setMessage("为了选择图片进行分析，应用需要访问您的设备存储。")
            .setPositiveButton("好的") { _, _ ->
                if (permission == Manifest.permission.READ_EXTERNAL_STORAGE) {
                    requestStoragePermissionLauncher.launch(permission) // 用户同意后再次请求权限
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun observeViewModelChanges() { // 观察ViewModel的LiveData变化
        viewModel.selectedImageUri.observe(this) { uri -> // 观察选择的图片URI
            if (uri != null) { // 如果已选择图片
                binding.imageViewPreview.load(uri) { // Coil加载图片
                    crossfade(true)
                    placeholder(android.R.drawable.ic_menu_gallery) // 加载中占位图
                    error(android.R.drawable.ic_menu_report_image)   // 错误占位图
                }
                binding.buttonAnalyzeImage.isEnabled = true // 分析按钮可用
                binding.textViewAnalysisResult.text = "" // 清空上次的分析结果 (使用统一的TextView ID)
            } else { // 未选择图片
                binding.imageViewPreview.setImageResource(android.R.drawable.ic_menu_gallery) // 重置预览图
                binding.buttonAnalyzeImage.isEnabled = false // 分析按钮不可用
                binding.textViewAnalysisResult.text = "" // 清空结果区
            }
        }

        viewModel.isLoading.observe(this) { isLoading -> // 观察加载状态
            binding.progressBarImageAnalysis.visibility = if (isLoading) View.VISIBLE else View.GONE // 显示/隐藏进度条
            binding.buttonSelectImage.isEnabled = !isLoading // 加载时禁用选择按钮
            binding.buttonAnalyzeImage.isEnabled = !isLoading && (viewModel.selectedImageUri.value != null) // 分析按钮可用条件
        }

        viewModel.analysisText.observe(this) { result -> // <<< 修改：观察合并后的 analysisText LiveData
            binding.textViewAnalysisResult.text = result ?: "等待AI宠物的看法..." // 将Kimi Vision的回复设置到统一的TextView
        }

        viewModel.errorMessage.observe(this) { errorMessage -> // 观察错误消息
            errorMessage?.let {
                Toast.makeText(this, "提示: $it", Toast.LENGTH_LONG).show() // 显示错误Toast
                Log.e(TAG, "错误信息: $it") // 记录错误日志
                viewModel.clearErrorMessage() // 清除错误，避免重复提示
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { // 处理Toolbar返回按钮
        finish() // 关闭当前Activity
        return true
    }
}