package com.dualtools.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dualtools.database.*
import kotlinx.coroutines.launch

class DatabaseViewModel : ViewModel() {
    private val dbManager = DatabaseManager()

    var selectedType by mutableStateOf(DatabaseType.MYSQL)
    var host by mutableStateOf("")
    var port by mutableStateOf("3306")
    var database by mutableStateOf("")
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var filePath by mutableStateOf("/sdcard/data.db")

    var isConnected by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var statusMessage by mutableStateOf("")
    var isError by mutableStateOf(false)

    var tables by mutableStateOf<List<TableInfo>>(emptyList())
    var selectedTable by mutableStateOf<String?>(null)
    var tableColumns by mutableStateOf<List<ColumnInfo>>(emptyList())
    var tableData by mutableStateOf<QueryResult?>(null)

    var sqlInput by mutableStateOf("")
    var sqlResult by mutableStateOf<QueryResult?>(null)

    // Visual CRUD state
    var crudMode by mutableStateOf(CrudMode.VIEW)
    var editRowData by mutableStateOf<Map<String, String>>(emptyMap())
    var whereCondition by mutableStateOf("")

    enum class CrudMode { VIEW, INSERT, UPDATE, DELETE }

    fun onTypeChanged(type: DatabaseType) {
        selectedType = type
        port = type.defaultPort.toString()
        if (type == DatabaseType.SQLITE) {
            host = ""
            database = ""
        }
    }

    fun connect() {
        val config = if (selectedType == DatabaseType.SQLITE) {
            DbConnection(type = selectedType, filePath = filePath)
        } else {
            DbConnection(
                type = selectedType,
                host = host.trim(),
                port = port.toIntOrNull() ?: selectedType.defaultPort,
                database = database.trim(),
                username = username.trim(),
                password = password
            )
        }
        isLoading = true
        statusMessage = "正在连接..."
        viewModelScope.launch {
            when (val result = dbManager.connect(config)) {
                is DbResult.Success -> {
                    isConnected = true
                    statusMessage = "已连接"
                    loadTables()
                }
                is DbResult.Error -> {
                    statusMessage = result.message
                    isError = true
                    isLoading = false
                }
            }
        }
    }

    fun disconnect() {
        dbManager.disconnect()
        isConnected = false
        tables = emptyList()
        tableData = null
        sqlResult = null
        statusMessage = ""
    }

    fun loadTables() {
        isLoading = true
        viewModelScope.launch {
            when (val result = dbManager.getTables()) {
                is DbResult.Success -> {
                    tables = result.data
                    statusMessage = "${tables.size} 张表"
                    isError = false
                }
                is DbResult.Error -> {
                    statusMessage = result.message
                    isError = true
                }
            }
            isLoading = false
        }
    }

    fun selectTable(tableName: String) {
        selectedTable = tableName
        isLoading = true
        viewModelScope.launch {
            // Load columns
            when (val colResult = dbManager.getTableColumns(tableName)) {
                is DbResult.Success -> tableColumns = colResult.data
                is DbResult.Error -> {
                    statusMessage = colResult.message
                    isError = true
                    isLoading = false
                    return@launch
                }
            }
            // Load data
            when (val dataResult = dbManager.getTableData(tableName)) {
                is DbResult.Success -> {
                    tableData = dataResult.data
                    statusMessage = "${dataResult.data.rowCount} 行"
                    isError = false
                }
                is DbResult.Error -> {
                    statusMessage = dataResult.message
                    isError = true
                }
            }
            isLoading = false
        }
    }

    fun executeSql() {
        if (sqlInput.isBlank()) return
        isLoading = true
        val sql = sqlInput.trim()
        viewModelScope.launch {
            when (val result = dbManager.executeQuery(sql)) {
                is DbResult.Success -> {
                    sqlResult = result.data
                    statusMessage = if (result.data.columns.firstOrNull() == "Affected Rows")
                        "影响 ${result.data.rows.firstOrNull()?.firstOrNull() ?: "0"} 行"
                    else
                        "${result.data.rowCount} 行, ${result.data.executionTimeMs}ms"
                    isError = false
                }
                is DbResult.Error -> {
                    statusMessage = result.message
                    isError = true
                }
            }
            isLoading = false
        }
    }

    fun insertRow(values: Map<String, String>) {
        val table = selectedTable ?: run {
            statusMessage = "未选择表"
            isError = true
            return
        }
        isLoading = true
        viewModelScope.launch {
            when (dbManager.insertRow(table, values)) {
                is DbResult.Success -> {
                    statusMessage = "插入成功"
                    isError = false
                    crudMode = CrudMode.VIEW
                    selectTable(table)
                }
                is DbResult.Error -> {
                    statusMessage = result.message
                    isError = true
                }
            }
            isLoading = false
        }
    }

    fun updateRow(values: Map<String, String>, where: String) {
        val table = selectedTable ?: run {
            statusMessage = "未选择表"
            isError = true
            return
        }
        isLoading = true
        viewModelScope.launch {
            when (dbManager.updateRow(table, values, where)) {
                is DbResult.Success -> {
                    statusMessage = "更新成功"
                    isError = false
                    crudMode = CrudMode.VIEW
                    selectTable(table)
                }
                is DbResult.Error -> {
                    statusMessage = result.message
                    isError = true
                }
            }
            isLoading = false
        }
    }

    fun deleteRow(where: String) {
        val table = selectedTable ?: run {
            statusMessage = "未选择表"
            isError = true
            return
        }
        isLoading = true
        viewModelScope.launch {
            when (dbManager.deleteRow(table, where)) {
                is DbResult.Success -> {
                    statusMessage = "删除成功"
                    isError = false
                    crudMode = CrudMode.VIEW
                    selectTable(table)
                }
                is DbResult.Error -> {
                    statusMessage = result.message
                    isError = true
                }
            }
            isLoading = false
        }
    }

    fun getConnectionInfo(): String {
        return if (selectedType == DatabaseType.SQLITE) {
            "SQLite: $filePath"
        } else {
            "$username@$host:$port/$database"
        }
    }
}
