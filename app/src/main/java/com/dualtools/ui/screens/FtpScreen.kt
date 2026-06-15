package com.dualtools.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dualtools.ftp.FtpFileItem
import com.dualtools.viewmodel.FtpViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FtpScreen(viewModel: FtpViewModel) {
    var showConnectionDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<FtpFileItem?>(null) }
    var selectedItems by remember { mutableStateOf<Set<String>>(emptySet()) }
    var newFolderName by remember { mutableStateOf("") }
    var renameText by remember { mutableStateOf("") }

    if (showConnectionDialog) {
        AlertDialog(
            onDismissRequest = { showConnectionDialog = false },
            title = { Text("FTP 连接") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = viewModel.host, onValueChange = { viewModel.host = it },
                        label = { Text("主机地址") }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = viewModel.port, onValueChange = { viewModel.port = it },
                        label = { Text("端口") }, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = viewModel.username, onValueChange = { viewModel.username = it },
                        label = { Text("用户名") }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = viewModel.password, onValueChange = { viewModel.password = it },
                        label = { Text("密码") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showConnectionDialog = false; viewModel.connect() }) {
                    Text("连接")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConnectionDialog = false }) { Text("取消") }
            }
        )
    }

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("新建文件夹") },
            text = {
                OutlinedTextField(
                    value = newFolderName, onValueChange = { newFolderName = it },
                    label = { Text("文件夹名") }, singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newFolderName.isNotBlank()) {
                        viewModel.createDirectory(newFolderName)
                        newFolderName = ""
                        showNewFolderDialog = false
                    }
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("取消") }
            }
        )
    }

    showRenameDialog?.let { item ->
        LaunchedEffect(item) { renameText = item.name }
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("重命名") },
            text = {
                OutlinedTextField(
                    value = renameText, onValueChange = { renameText = it },
                    label = { Text("新名称") }, singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (renameText.isNotBlank() && renameText != item.name) {
                        viewModel.renameItem(item, renameText)
                    }
                    showRenameDialog = null
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) { Text("取消") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Connection bar
        if (viewModel.isConnected) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = viewModel.getConnectionInfo(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = viewModel.currentPath,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = { viewModel.disconnect() }) {
                        Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("断开")
                    }
                }
            }
        }

        // Toolbar
        if (viewModel.isConnected) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (viewModel.currentPath != "/") {
                    IconButton(onClick = { viewModel.navigateTo("..") }) {
                        Icon(Icons.Default.ArrowUpward, "上级目录")
                    }
                }
                IconButton(onClick = { viewModel.loadFiles() }) {
                    Icon(Icons.Default.Refresh, "刷新")
                }
                IconButton(onClick = { showNewFolderDialog = true }) {
                    Icon(Icons.Default.CreateNewFolder, "新建文件夹")
                }
                if (selectedItems.isNotEmpty()) {
                    IconButton(onClick = {
                        viewModel.copyItems(selectedItems.toList())
                        selectedItems = emptySet()
                    }) {
                        Icon(Icons.Default.ContentCopy, "复制")
                    }
                    IconButton(onClick = {
                        viewModel.cutItems(selectedItems.toList())
                        selectedItems = emptySet()
                    }) {
                        Icon(Icons.Default.ContentCut, "剪切")
                    }
                    IconButton(onClick = {
                        selectedItems.forEach { path ->
                            val item = viewModel.files.find { it.path == path }
                            if (item != null) viewModel.deleteItem(item)
                        }
                        selectedItems = emptySet()
                    }) {
                        Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error)
                    }
                }
                if (viewModel.hasClipboard()) {
                    IconButton(onClick = { viewModel.pasteItems() }) {
                        Icon(Icons.Default.ContentPaste, "粘贴")
                    }
                }
            }
        }

        // Status bar
        if (viewModel.statusMessage.isNotBlank()) {
            Surface(
                color = if (viewModel.isError) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = viewModel.statusMessage,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Content
        if (viewModel.isConnected) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(viewModel.files, key = { it.path }) { file ->
                    val isSelected = selectedItems.contains(file.path)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selectedItems.isNotEmpty()) {
                                    selectedItems = if (isSelected)
                                        selectedItems - file.path
                                    else
                                        selectedItems + file.path
                                } else if (file.isDirectory) {
                                    viewModel.navigateTo(file.name)
                                }
                            },
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = if (file.isDirectory) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                if (!file.isDirectory) {
                                    Text(
                                        text = "${formatSize(file.size)}  ${file.lastModified}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (!file.isDirectory) {
                                Text(
                                    text = formatSize(file.size),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                            // Long press actions
                            IconButton(onClick = {
                                if (file.isDirectory) viewModel.navigateTo(file.name)
                            }) {
                                Icon(Icons.Default.OpenInNew, "打开", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { showRenameDialog = file }) {
                                Icon(Icons.Default.Edit, "重命名", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { viewModel.deleteItem(file) }) {
                                Icon(
                                    Icons.Default.Delete, "删除", modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    Divider(thickness = 0.5.dp)
                }
            }
        } else {
            // Not connected
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Dns, contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "未连接到 FTP 服务器",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { showConnectionDialog = true }) {
                        Icon(Icons.Default.Link, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("连接 FTP 服务器")
                    }
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
        else -> "${"%.2f".format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
    }
}
