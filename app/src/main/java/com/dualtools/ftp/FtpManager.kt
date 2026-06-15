package com.dualtools.ftp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class FtpConnection(
    val host: String,
    val port: Int = 21,
    val username: String = "anonymous",
    val password: String = ""
)

data class FtpFileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: String
)

sealed class FtpResult<out T> {
    data class Success<T>(val data: T) : FtpResult<T>()
    data class Error(val message: String) : FtpResult<Nothing>()
}

class FtpManager {
    private val ftpClient = FTPClient()
    private var connection: FtpConnection? = null
    private var currentPath = "/"

    val isConnected: Boolean get() = ftpClient.isConnected

    suspend fun connect(conn: FtpConnection): FtpResult<Unit> = withContext(Dispatchers.IO) {
        try {
            ftpClient.connect(conn.host, conn.port)
            val reply = ftpClient.replyCode
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect()
                return@withContext FtpResult.Error("连接失败: $reply")
            }
            if (!ftpClient.login(conn.username, conn.password)) {
                ftpClient.disconnect()
                return@withContext FtpResult.Error("登录失败")
            }
            ftpClient.enterLocalPassiveMode()
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
            ftpClient.setControlEncoding("UTF-8")
            connection = conn
            currentPath = ftpClient.printWorkingDirectory() ?: "/"
            FtpResult.Success(Unit)
        } catch (e: Exception) {
            FtpResult.Error("连接异常: ${e.message}")
        }
    }

    fun disconnect() {
        try {
            ftpClient.logout()
            ftpClient.disconnect()
        } catch (_: Exception) {}
        connection = null
        currentPath = "/"
    }

    suspend fun listFiles(path: String? = null): FtpResult<List<FtpFileItem>> = withContext(Dispatchers.IO) {
        try {
            val targetPath = path ?: currentPath
            val files = ftpClient.listFiles(targetPath)
            if (files == null) return@withContext FtpResult.Error("无法列出目录")

            val items = files.map { file ->
                FtpFileItem(
                    name = file.name,
                    path = if (targetPath.endsWith("/")) "$targetPath${file.name}" else "$targetPath/${file.name}",
                    isDirectory = file.isDirectory,
                    size = file.size,
                    lastModified = file.timestamp.formatted ?: "N/A"
                )
            }.sortedWith(compareByDescending<FtpFileItem> { it.isDirectory }.thenBy { it.name.lowercase() })

            currentPath = targetPath
            FtpResult.Success(items)
        } catch (e: Exception) {
            FtpResult.Error("列出文件失败: ${e.message}")
        }
    }

    suspend fun changeDirectory(dirName: String): FtpResult<List<FtpFileItem>> = withContext(Dispatchers.IO) {
        try {
            val newPath = if (dirName == "..") {
                currentPath.substringBeforeLast("/", "/").ifEmpty { "/" }
            } else {
                if (currentPath.endsWith("/")) "$currentPath$dirName" else "$currentPath/$dirName"
            }
            if (!ftpClient.changeWorkingDirectory(newPath)) {
                return@withContext FtpResult.Error("无法进入目录: $dirName")
            }
            currentPath = newPath
            listFiles(currentPath)
        } catch (e: Exception) {
            FtpResult.Error("切换目录失败: ${e.message}")
        }
    }

    suspend fun createDirectory(name: String): FtpResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val path = if (currentPath.endsWith("/")) "$currentPath$name" else "$currentPath/$name"
            if (!ftpClient.makeDirectory(path)) {
                return@withContext FtpResult.Error("创建文件夹失败")
            }
            FtpResult.Success(Unit)
        } catch (e: Exception) {
            FtpResult.Error("创建文件夹失败: ${e.message}")
        }
    }

    suspend fun deleteFile(path: String, isDirectory: Boolean): FtpResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val success = if (isDirectory) ftpClient.removeDirectory(path) else ftpClient.deleteFile(path)
            if (!success) return@withContext FtpResult.Error("删除失败")
            FtpResult.Success(Unit)
        } catch (e: Exception) {
            FtpResult.Error("删除失败: ${e.message}")
        }
    }

    suspend fun rename(fromPath: String, toPath: String): FtpResult<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!ftpClient.rename(fromPath, toPath)) {
                return@withContext FtpResult.Error("重命名失败")
            }
            FtpResult.Success(Unit)
        } catch (e: Exception) {
            FtpResult.Error("重命名失败: ${e.message}")
        }
    }

    suspend fun downloadFile(remotePath: String, localFile: File): FtpResult<Unit> = withContext(Dispatchers.IO) {
        try {
            FileOutputStream(localFile).use { fos ->
                if (!ftpClient.retrieveFile(remotePath, fos)) {
                    return@withContext FtpResult.Error("下载失败")
                }
            }
            FtpResult.Success(Unit)
        } catch (e: Exception) {
            FtpResult.Error("下载失败: ${e.message}")
        }
    }

    suspend fun uploadFile(localFile: File, remoteName: String): FtpResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val remotePath = if (currentPath.endsWith("/")) "$currentPath$remoteName" else "$currentPath/$remoteName"
            FileInputStream(localFile).use { fis ->
                if (!ftpClient.storeFile(remotePath, fis)) {
                    return@withContext FtpResult.Error("上传失败")
                }
            }
            FtpResult.Success(Unit)
        } catch (e: Exception) {
            FtpResult.Error("上传失败: ${e.message}")
        }
    }

    fun getCurrentPath(): String = currentPath
}
