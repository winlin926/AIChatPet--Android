package com.example.aichatpet.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.aichatpet.BuildConfig
// 移除了 Baidu 相关的 import: com.example.aichatpet.data.model.baidu.RecognizedObject
import com.example.aichatpet.data.remote.dto.ApiMessage // 确保这个是修改后的版本，content类型为Any
import com.example.aichatpet.data.remote.dto.ChatCompletionRequest
import com.example.aichatpet.data.network.ApiClient
import com.example.aichatpet.ui.auth.RegisterActivity // 用于获取SharedPreferences常量
import com.example.aichatpet.ui.settings.ApiConfigActivity // 用于获取SharedPreferences常量
import com.example.aichatpet.data.remote.dto.ImageUrl
import com.example.aichatpet.data.remote.dto.ImageUrlContentPart
import com.example.aichatpet.data.remote.dto.TextContentPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Locale

class ImageAnalysisViewModel(application: Application) : AndroidViewModel(application) { // ViewModel for ImageAnalysisActivity, now using Kimi Vision API

    private val userPrefs: SharedPreferences = application.getSharedPreferences( // 用于存储用户相关偏好设置 (如宠物名)
        RegisterActivity.PREFS_NAME,
        Context.MODE_PRIVATE
    )
    private val apiConfigPrefs: SharedPreferences = application.getSharedPreferences( // 用于存储API相关配置
        ApiConfigActivity.PREFS_API_CONFIG,
        Context.MODE_PRIVATE
    )

    private val moonshotApiKey: String // Moonshot (Kimi AI) API Key
        get() { // 计算属性
            return apiConfigPrefs.getString(ApiConfigActivity.KEY_KIMI_API_KEY, null)
                ?.takeIf { it.isNotBlank() }
                ?: BuildConfig.MOONSHOT_API_KEY // 回退到BuildConfig
        }

    private var currentPetName: String = "" // 当前宠物名称

    private val _selectedImageUri = MutableLiveData<Uri?>() // 用户选择的图片URI
    val selectedImageUri: LiveData<Uri?> = _selectedImageUri

    private val _isLoading = MutableLiveData<Boolean>(false) // 加载状态
    val isLoading: LiveData<Boolean> = _isLoading

    // 合并为一个分析结果文本 LiveData
    private val _analysisText = MutableLiveData<String>("") // Kimi对图片的分析/描述/评论
    val analysisText: LiveData<String> = _analysisText

    private val _errorMessage = MutableLiveData<String?>() // 错误消息
    val errorMessage: LiveData<String?> = _errorMessage

    init { // ViewModel 初始化
        currentPetName = getCurrentPetNameFromPrefs() // 加载宠物名
        Log.d("ImageAnalysisVM", "Initialized with pet name: $currentPetName. Kimi Key configured: ${moonshotApiKey.isNotBlank()}") // 初始化日志
    }

    private fun getCurrentPetNameFromPrefs(): String { // 从SharedPreferences读取宠物名
        return userPrefs.getString(RegisterActivity.KEY_PET_NAME, RegisterActivity.DEFAULT_PET_NAME)
            ?: RegisterActivity.DEFAULT_PET_NAME // 如果为null则返回默认宠物名
    }

    fun updatePetName() { // 更新宠物名
        currentPetName = getCurrentPetNameFromPrefs()
        Log.d("ImageAnalysisVM", "Updated pet name: $currentPetName")
    }

    fun setSelectedImageUri(uri: Uri?) { // 设置选择的图片URI
        _selectedImageUri.value = uri // 更新LiveData
        if (uri == null) { // 如果URI为空，清空结果
            _analysisText.value = ""
            _errorMessage.value = null
        }
    }

    fun analyzeSelectedImage() { // 使用Kimi Vision API分析选定的图片
        val imageUri = _selectedImageUri.value ?: run { // 获取图片URI，为空则返回
            _errorMessage.value = "请先选择一张图片进行分析。" // 设置错误消息
            return
        }

        val currentKimiApiKey = moonshotApiKey // 获取Kimi API Key
        if (currentKimiApiKey.isBlank()) { // 检查Key是否配置
            _errorMessage.value = "Kimi (Moonshot) API Key 未配置。"
            _analysisText.value = "分析失败：应用API Key配置错误"
            _isLoading.value = false // 重置加载状态
            return
        }

        _isLoading.value = true // 开始分析，设置加载状态
        _errorMessage.value = null // 清除旧错误
        _analysisText.value = "${currentPetName}正在看图并思考怎么说..." // 设置处理中提示

        viewModelScope.launch { // 启动协程执行分析流程
            try {
                val imageDataUrl = convertUriToDataUrlSuspend(imageUri) // 将图片URI转换为 "data:image/..." Base64字符串
                if (imageDataUrl == null) { // 如果转换失败
                    _errorMessage.value = "图片处理失败，无法转换为API所需格式。"
                    _analysisText.value = "分析失败：图片处理错误"
                    _isLoading.value = false
                    return@launch
                }

                val messagesForKimiVision = listOf( // 构建发送给Kimi Vision API的消息列表
                    ApiMessage( // 系统消息，设定AI角色和名称
                        role = "system",
                        content = "你是名叫“${currentPetName}”的AI宠物，你的性格活泼可爱、充满好奇心。你会用简洁、友好、俏皮的语气与用户对话，并能描述图片内容。"
                    ),
                    ApiMessage( // 用户消息，包含图片数据和文本提示
                        role = "user",
                        content = listOf( // content现在是一个包含图片和文本部分的列表
                            ImageUrlContentPart(imageUrl = ImageUrl(url = imageDataUrl)), // 图片部分
                            TextContentPart(text = "请你帮我看看这张图片里有什么呀？并用你的风格评论一下吧！") // 文本指令部分
                        )
                    )
                )

                val request = ChatCompletionRequest( // 构建API请求体
                    model = "moonshot-v1-8k-vision-preview", // 指定使用Kimi的Vision模型 (注意这里与ChatViewModel中模型不同)
                    messages = messagesForKimiVision,
                    temperature = 0.7 // 可根据需要调整回复的随机性
                )

                Log.d("ImageAnalysisVM", "调用 Kimi Vision API (getChatCompletions)...") // 日志记录
                val response = ApiClient.moonshotApiService.getChatCompletions( // 调用Moonshot API服务
                    apiKey = "Bearer $currentKimiApiKey", // API Key，ViewModel负责添加 "Bearer "前缀
                    requestBody = request // 请求体
                )

                if (response.isSuccessful && response.body() != null) { // 如果API请求成功且响应体不为空
                    val kimiResult = response.body()!! // 获取响应体
                    val petReply = kimiResult.choices?.firstOrNull()?.message?.content as? String // Kimi Vision回复的content通常是String
                    if (!petReply.isNullOrBlank()) { // 如果成功解析到回复内容
                        _analysisText.value = petReply // 将Kimi的回复直接作为分析结果和宠物评论
                        Log.i("ImageAnalysisVM", "Kimi Vision API Success: $petReply")
                    } else { // 如果响应成功但未能解析到具体回复内容
                        val errorMsgFromApi = kimiResult.error?.message ?: "未能获取有效的分析结果。"
                        _analysisText.value = "${currentPetName}：嗯... 这张图有点难倒我了。($errorMsgFromApi)"
                        _errorMessage.value = "Kimi Vision API 返回空回复: $errorMsgFromApi"
                        Log.e("ImageAnalysisVM", "Kimi Vision API Success but no valid content: ${kimiResult.error}")
                    }
                } else { // 如果Kimi API的HTTP请求本身失败
                    val errorBody = response.errorBody()?.string() // 获取原始错误响应体
                    val errorCode = response.code() // 获取HTTP状态码
                    val apiErrorMessage = response.body()?.error?.message ?: response.message() // 尝试从响应体或响应消息中获取错误文本
                    _analysisText.value = "${currentPetName}：呜，我的AI大脑看图功能暂时出了点小状况 ($errorCode)"
                    _errorMessage.value = "Kimi Vision API 请求失败: $errorCode - $apiErrorMessage $errorBody"
                    Log.e("ImageAnalysisVM", "Kimi Vision API HTTP Error: $errorCode - $errorBody; API specific error: $apiErrorMessage")
                }

            } catch (e: Exception) { // 捕获流程中发生的任何其他异常
                _analysisText.value = "${currentPetName}：分析图片的时候我好像走神了... (${e.localizedMessage})"
                _errorMessage.value = "程序异常: ${e.localizedMessage}"
                Log.e("ImageAnalysisVM", "Exception during Kimi Vision image analysis", e)
            } finally { // 无论分析流程是否成功都会执行
                _isLoading.value = false // 分析流程完成后，将加载状态恢复为false
            }
        }
    }

    private suspend fun convertUriToDataUrlSuspend(uri: Uri): String? { // 将图片URI转换为包含MIME类型和Base64前缀的Data URL字符串
        return withContext(Dispatchers.IO) { // 将文件操作和图片编解码切换到IO分发器
            try {
                val context = getApplication<Application>().applicationContext // 获取应用的上下文
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri) // 通过ContentResolver从URI打开一个输入流
                val originalBitmap = BitmapFactory.decodeStream(inputStream) // 从输入流中解码生成原始的Bitmap对象
                inputStream?.close() // 关闭输入流，释放资源

                if (originalBitmap == null) { // 如果无法从URI解码出Bitmap
                    Log.e("ImageAnalysisVM", "无法从 URI 解码 Bitmap。")
                    return@withContext null // 返回null表示转换失败
                }

                val imageType = context.contentResolver.getType(uri)?.substringAfter("image/")?.lowercase(Locale.getDefault()) ?: "jpeg" // 尝试获取MIME类型确定图片格式，默认为jpeg
                val compressFormat = when (imageType) { // 根据图片类型选择Bitmap的压缩格式
                    "png" -> Bitmap.CompressFormat.PNG
                    "webp" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.JPEG // WEBP_LOSSY需要API 30+
                    else -> Bitmap.CompressFormat.JPEG // 默认为JPEG
                }

                var quality = 90 // 初始压缩质量 (主要对JPEG有效)
                val outputStream = ByteArrayOutputStream() // 用于接收压缩后的图片数据
                originalBitmap.compress(compressFormat, quality, outputStream) // 压缩Bitmap

                if (compressFormat == Bitmap.CompressFormat.JPEG) { // 对JPEG进行循环压缩以控制大小
                    while (outputStream.size() / (1024 * 1024.0) > 2.8 && quality > 10) { // 目标原始JPEG文件大小小于约2.8MB
                        outputStream.reset() // 重置输出流
                        quality -= 10 // 降低质量
                        originalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream) // 重新压缩
                    }
                }
                Log.d("ImageAnalysisVM", "图片压缩后大小: ${outputStream.size()} 字节, 格式: $compressFormat, 质量(JPEG时): $quality")

                val imageBytes = outputStream.toByteArray() // 获取压缩后的字节数组
                val base64EncodedImage = Base64.encodeToString(imageBytes, Base64.NO_WRAP) // Base64编码 (NO_WRAP避免插入换行符)

                "data:image/$imageType;base64,$base64EncodedImage" // 返回Kimi要求的Data URL格式
            } catch (e: IOException) { // 捕获IO异常
                Log.e("ImageAnalysisVM", "文件操作或图片转换失败 (IOException)", e)
                null
            } catch (e: Exception) { // 捕获其他通用异常
                Log.e("ImageAnalysisVM", "URI 转 Data URL Base64 失败 (Exception)", e)
                null
            }
        }
    }

    fun clearErrorMessage() { // 清除当前的错误消息状态
        _errorMessage.value = null // 将错误消息LiveData的值设置为null
    }
}