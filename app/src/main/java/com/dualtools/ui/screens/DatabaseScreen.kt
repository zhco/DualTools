package com.dualtools.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.dualtools.database.DatabaseType
import com.dualtools.viewmodel.DatabaseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseScreen(viewModel: DatabaseViewModel) {
    var showConnectionDialog by remember { mutableStateOf(false) }
    var showInsertDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var insertValues by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var updateValues by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var updateWhere by remember { mutableStateOf("") }
    var deleteWhere by remember { mutableStateOf("") }
    var typeExpanded by remember { mutableStateOf(false) }

    // Connection dialog
    if (showConnectionDialog) {
        AlertDialog(
            onDismissRequest = { showConnectionDialog = false },
            title = { Text("数据库连接") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Database type selector
                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = viewModel.selectedType.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("数据库类型") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false }
                        ) {
                            DatabaseType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.displayName) },
                                    onClick = {
                                        viewModel.onTypeChanged(type)
                                        typeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = viewModel.host, onValueChange = { viewModel.host = it },
                        label = { Text("主机") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    OutlinedTextField(
                        value = viewModel.port, onValueChange = { viewModel.port = it },
                        label = { Text("端口") }, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
                    )
                    OutlinedTextField(
                        value = viewModel.database, onValueChange = { viewModel.database = it },
                        label = { Text("数据库名") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    OutlinedTextField(
                        value = viewModel.username, onValueChange = { viewModel.username = it },
                        label = { Text("用户名") }, modifier = Modifier.fillMaxWidth(), singleLine = true
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

    // Insert dialog
    if (showInsertDialog) {
        AlertDialog(
            onDismissRequest = { showInsertDialog = false },
            title = { Text("插入数据 - ${viewModel.selectedTable}") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(viewModel.tableColumns) { col ->
                        if (!col.isPrimaryKey) {
                            OutlinedTextField(
                                value = insertValues[col.name] ?: "",
                                onValueChange = { v ->
                                    insertValues = insertValues + (col.name to v)
                                },
                                label = { Text("${col.name} (${col.type})") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.insertRow(insertValues)
                    insertValues = emptyMap()
                    showInsertDialog = false
                }) { Text("插入") }
            },
            dismissButton = {
                TextButton(onClick = { showInsertDialog = false; insertValues = emptyMap() }) { Text("取消") }
            }
        )
    }

    // Update dialog
    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("更新数据 - ${viewModel.selectedTable}") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(viewModel.tableColumns) { col ->
                        OutlinedTextField(
                            value = updateValues[col.name] ?: "",
                            onValueChange = { v ->
                                updateValues = updateValues + (col.name to v)
                            },
                            label = { Text("${col.name} (${col.type})") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = updateWhere,
                            onValueChange = { updateWhere = it },
                            label = { Text("WHERE 条件 (如 id=1)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateRow(updateValues, updateWhere)
                    updateValues = emptyMap()
                    updateWhere = ""
                    showUpdateDialog = false
                }) { Text("更新") }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false; updateValues = emptyMap() }) { Text("取消") }
            }
        )
    }

    // Delete dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除数据 - ${viewModel.selectedTable}") },
            text = {
                OutlinedTextField(
                    value = deleteWhere,
                    onValueChange = { deleteWhere = it },
                    label = { Text("WHERE 条件 (如 id=5)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRow(deleteWhere)
                        deleteWhere = ""
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = viewModel.getConnectionInfo(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    TextButton(onClick = { viewModel.disconnect() }) {
                        Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("断开")
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
                    Text(text = viewModel.statusMessage, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (viewModel.isConnected) {
            // Tab row: Tables | SQL | Data
            var dbTabIndex by remember { mutableIntStateOf(0) }
            TabRow(selectedTabIndex = dbTabIndex) {
                Tab(selected = dbTabIndex == 0, onClick = { dbTabIndex = 0 }, text = { Text("表 & 数据") })
                Tab(selected = dbTabIndex == 1, onClick = { dbTabIndex = 1 }, text = { Text("SQL 编辑器") })
            }

            when (dbTabIndex) {
                0 -> TablesDataView(
                    viewModel = viewModel,
                    onInsertClick = { showInsertDialog = true },
                    onUpdateClick = {
                        updateValues = emptyMap()
                        updateWhere = ""
                        showUpdateDialog = true
                    },
                    onDeleteClick = { showDeleteDialog = true }
                )
                1 -> SqlEditorView(viewModel)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Storage, contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("未连接到数据库", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { showConnectionDialog = true }) {
                        Icon(Icons.Default.Link, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("连接数据库")
                    }
                }
            }
        }
    }
}

@Composable
private fun TablesDataView(
    viewModel: DatabaseViewModel,
    onInsertClick: () -> Unit,
    onUpdateClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Table list sidebar
        Surface(
            modifier = Modifier.width(140.dp).fillMaxHeight(),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            LazyColumn {
                item {
                    TextButton(
                        onClick = { viewModel.loadTables() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("刷新", style = MaterialTheme.typography.labelSmall)
                    }
                }
                items(viewModel.tables) { table ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.selectTable(table.name) },
                        color = if (viewModel.selectedTable == table.name)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.TableChart, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = table.name,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Data view
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (viewModel.selectedTable != null) {
                // CRUD buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = onInsertClick,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("新增", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = onUpdateClick,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("更新", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = onDeleteClick,
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("删除", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Table data
                viewModel.tableData?.let { data ->
                    if (data.columns.isNotEmpty()) {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .horizontalScroll(scrollState)
                        ) {
                            // Header
                            Row(
                                modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                data.columns.forEach { col ->
                                    Text(
                                        text = col,
                                        modifier = Modifier.width(120.dp).padding(6.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            // Rows
                            LazyColumn {
                                items(data.rows) { row ->
                                    Row {
                                        row.forEachIndexed { _, cell ->
                                            Text(
                                                text = cell,
                                                modifier = Modifier.width(120.dp).padding(4.dp),
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    Divider(thickness = 0.5.dp)
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } ?: Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("选择一张表查看数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("从左侧选择一张表", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SqlEditorView(viewModel: DatabaseViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SQL 编辑器", style = MaterialTheme.typography.titleSmall)
            Button(
                onClick = { viewModel.executeSql() },
                enabled = viewModel.sqlInput.isNotBlank() && !viewModel.isLoading
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("执行")
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = viewModel.sqlInput,
            onValueChange = { viewModel.sqlInput = it },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            textStyle = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            ),
            placeholder = { Text("输入 SQL 语句...\n例如: SELECT * FROM users LIMIT 10") },
            maxLines = 8
        )

        Spacer(Modifier.height(8.dp))

        // Result
        viewModel.sqlResult?.let { result ->
            Text(
                "${result.rowCount} 行, ${result.executionTimeMs}ms",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))

            if (result.columns.isNotEmpty()) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .horizontalScroll(scrollState)
                ) {
                    Row(
                        modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        result.columns.forEach { col ->
                            Text(
                                text = col,
                                modifier = Modifier.width(120.dp).padding(6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    LazyColumn {
                        items(result.rows) { row ->
                            Row {
                                row.forEach { cell ->
                                    Text(
                                        text = cell,
                                        modifier = Modifier.width(120.dp).padding(4.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Divider(thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

