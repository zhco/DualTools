package com.dualtools.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dualtools.ftp.*
import kotlinx.coroutines.launch

class FtpViewModel : ViewModel() {
    private val ftpManager = FtpManager()

    var host by mutableStateOf("")
    var port by mutableStateOf("21")
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var currentPath by mutableStateOf("/")
    var files by mutableStateOf<List<FtpFileItem>>(emptyList())
    var isConnected by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var statusMessage by mutableStateOf("")
    var isError by mutableStateOf(false)

    // Clipboard for copy/paste
    private var clipboardPaths: List<String> = emptyList()
    private var clipboardCutMode = false

    fun connect() {
        val conn = FtpConnection(
            host = host.trim(),
            port = port.toIntOrNull() ?: 21,
            username = username.trim().ifEmpty { "anonymous" },
            password = password
        )
        isLoading = true
        statusMessage = "正在连接..."
        viewModelScope.launch {
            when (val result = ftpManager.connect(conn)) {
                is FtpResult.Success -> {
                    isConnected = true
                    currentPath = ftpManager.getCurrentPath()
                    loadFiles()
                }
                is FtpResult.Error -> {
                    statusMessage = result.message
                    isError = true
                    isLoading = false
                }
            }
        }
    }

    fun disconnect() {
        ftpManager.disconnect()
        isConnected = false
        files = emptyList()
        currentPath = "/"
        statusMessage = ""
    }

    fun loadFiles() {
        isLoading = true
        viewModelScope.launch {
            when (val result = ftpManager.listFiles()) {
                is FtpResult.Success -> {
                    files = result.data
                    currentPath = ftpManager.getCurrentPath()
                    statusMessage = "${files.size} 个项目"
                    isError = false
                }
                is FtpResult.Error -> {
                    statusMessage = result.message
                    isError = true
                }
            }
            isLoading = false
        }
    }

    fun navigateTo(dirName: String) {
        isLoading = true
        viewModelScope.launch {
            when (val result = ftpManager.changeDirectory(dirName)) {
                is FtpResult.Success -> {
                    files = result.data
                    currentPath = ftpManager.getCurrentPath()
                    statusMessage = "${files.size} 个项目"
                    isError = false
                }
                is FtpResult.Error -> {
                    statusMessage = result.message
                    isError = true
                }
            }
            isLoading = false
        }
    }

    fun createDirectory(name: String) {
        isLoading = true
        viewModelScope.launch {
            when (val result = ftpManager.createDirectory(name)) {
                is FtpResult.Success -> {
                    statusMessage = "文件夹已创建"
                    isError = false
                    loadFiles()
                }
                is FtpResult.Error -> {
                    statusMessage = result.message
                    isError = true
                }
            }
            isLoading = false
        }
    }

    fun deleteItem(item: FtpFileItem) {
        isLoading = true
        viewModelScope.launch {
            when (val result = ftpManager.deleteFile(item.path, item.isDirectory)) {
                is FtpResult.Success -> {
                    statusMessage = "已删除: ${item.name}"
                    isError = false
                    loadFiles()
                }
                is FtpResult.Error -> {
                    statusMessage = result.message
                    isError = true
                }
            }
            isLoading = false
        }
    }

    fun renameItem(item: FtpFileItem, newName: String) {
        val parentPath = item.path.substringBeforeLast("/", "/")
        val newPath = if (parentPath.endsWith("/")) "$parentPath$newName" else "$parentPath/$newName"
        isLoading = true
        viewModelScope.launch {
            when (val result = ftpManager.rename(item.path, newPath)) {
                is FtpResult.Success -> {
                    statusMessage = "已重命名"
                    isError = false
                    loadFiles()
                }
                is FtpResult.Error -> {
                    statusMessage = result.message
                    isError = true
                }
            }
            isLoading = false
        }
    }

    fun copyItems(paths: List<String>) {
        clipboardPaths = paths
        clipboardCutMode = false
        statusMessage = "已复制 ${paths.size} 个项目"
    }

    fun cutItems(paths: List<String>) {
        clipboardPaths = paths
        clipboardCutMode = true
        statusMessage = "已剪切 ${paths.size} 个项目"
    }

    fun pasteItems() {
        if (clipboardPaths.isEmpty()) return
        isLoading = true
        viewModelScope.launch {
            var success = 0
            var failed = 0
            for (path in clipboardPaths) {
                val name = path.substringAfterLast("/")
                val result = ftpManager.rename(path, "${currentPath}/$name")
                if (result is FtpResult.Success) success++ else failed++
            }
            if (!clipboardCutMode) {
                clipboardPaths = emptyList()
            }
            statusMessage = "完成: $success 成功, $failed 失败"
            isError = failed > 0
            loadFiles()
            isLoading = false
        }
    }

    fun hasClipboard(): Boolean = clipboardPaths.isNotEmpty()

    fun getConnectionInfo(): String {
        return "$username@$host:$port"
    }
}
