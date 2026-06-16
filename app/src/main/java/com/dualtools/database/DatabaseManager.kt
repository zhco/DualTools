package com.dualtools.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.*

enum class DatabaseType(val displayName: String, val defaultPort: Int, val driverClass: String) {
    MYSQL("MySQL", 3306, "com.mysql.cj.jdbc.Driver"),
    POSTGRESQL("PostgreSQL", 5432, "org.postgresql.Driver"),
    SQLITE("SQLite", 0, "org.sqlite.JDBC"),
    SQLSERVER("SQL Server", 1433, "com.microsoft.sqlserver.jdbc.SQLServerDriver")
    // Oracle 驱动不兼容 Android，已移除
}

data class DbConnection(
    val type: DatabaseType,
    val host: String = "",
    val port: Int = 0,
    val database: String = "",
    val username: String = "",
    val password: String = "",
    val filePath: String = "" // For SQLite
)

data class TableInfo(
    val name: String,
    val schema: String = ""
)

data class ColumnInfo(
    val name: String,
    val type: String,
    val nullable: Boolean,
    val isPrimaryKey: Boolean
)

data class QueryResult(
    val columns: List<String>,
    val rows: List<List<String>>,
    val rowCount: Int,
    val executionTimeMs: Long
)

sealed class DbResult<out T> {
    data class Success<T>(val data: T) : DbResult<T>()
    data class Error(val message: String) : DbResult<Nothing>()
}

class DatabaseManager {
    private var connection: Connection? = null
    private var dbType: DatabaseType? = null

    val isConnected: Boolean
        get() = connection?.isClosed?.not() == true

    suspend fun connect(config: DbConnection): DbResult<Unit> = withContext(Dispatchers.IO) {
        try {
            // 安全加载驱动，单个驱动失败不影响其他
            try {
                Class.forName(config.type.driverClass)
            } catch (e: Throwable) {
                return@withContext DbResult.Error("驱动加载失败: ${config.type.displayName} 驱动在当前设备不可用 (${e.javaClass.simpleName})")
            }

            val url = when (config.type) {
                DatabaseType.MYSQL ->
                    "jdbc:mysql://${config.host}:${config.port}/${config.database}?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8"
                DatabaseType.POSTGRESQL ->
                    "jdbc:postgresql://${config.host}:${config.port}/${config.database}"
                DatabaseType.SQLITE ->
                    "jdbc:sqlite:${config.filePath}"
                DatabaseType.SQLSERVER ->
                    "jdbc:sqlserver://${config.host}:${config.port};databaseName=${config.database};encrypt=false"
            }

            connection = DriverManager.getConnection(url, config.username, config.password)
            dbType = config.type
            DbResult.Success(Unit)
        } catch (e: SQLException) {
            DbResult.Error("数据库连接失败: ${e.message}")
        } catch (e: Throwable) {
            DbResult.Error("连接异常: ${e.message}")
        }
    }

    fun disconnect() {
        try {
            connection?.close()
        } catch (_: Exception) {}
        connection = null
        dbType = null
    }

    suspend fun getTables(): DbResult<List<TableInfo>> = withContext(Dispatchers.IO) {
        try {
            val conn = connection ?: return@withContext DbResult.Error("未连接数据库")
            val tables = mutableListOf<TableInfo>()
            val metaData = conn.metaData

            val catalog = if (dbType == DatabaseType.MYSQL || dbType == DatabaseType.POSTGRESQL)
                conn.catalog else null
            val schema = if (dbType == DatabaseType.POSTGRESQL) "public" else null

            val rs = metaData.getTables(catalog, schema, "%", arrayOf("TABLE"))
            while (rs.next()) {
                tables.add(TableInfo(name = rs.getString("TABLE_NAME")))
            }
            rs.close()
            DbResult.Success(tables)
        } catch (e: Throwable) {
            DbResult.Error("获取表列表失败: ${e.message}")
        }
    }

    suspend fun getTableColumns(tableName: String): DbResult<List<ColumnInfo>> = withContext(Dispatchers.IO) {
        try {
            val conn = connection ?: return@withContext DbResult.Error("未连接数据库")
            val columns = mutableListOf<ColumnInfo>()
            val metaData = conn.metaData

            // Get primary keys
            val pkRs = metaData.getPrimaryKeys(null, null, tableName)
            val pkColumns = mutableSetOf<String>()
            while (pkRs.next()) {
                pkColumns.add(pkRs.getString("COLUMN_NAME"))
            }
            pkRs.close()

            val rs = metaData.getColumns(null, null, tableName, "%")
            while (rs.next()) {
                val colName = rs.getString("COLUMN_NAME")
                columns.add(
                    ColumnInfo(
                        name = colName,
                        type = rs.getString("TYPE_NAME"),
                        nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable,
                        isPrimaryKey = pkColumns.contains(colName)
                    )
                )
            }
            rs.close()
            DbResult.Success(columns)
        } catch (e: Throwable) {
            DbResult.Error("获取列信息失败: ${e.message}")
        }
    }

    suspend fun executeQuery(sql: String): DbResult<QueryResult> = withContext(Dispatchers.IO) {
        try {
            val conn = connection ?: return@withContext DbResult.Error("未连接数据库")
            val startTime = System.currentTimeMillis()

            val stmt = conn.createStatement()
            val isSelect = sql.trim().uppercase().startsWith("SELECT") ||
                    sql.trim().uppercase().startsWith("SHOW") ||
                    sql.trim().uppercase().startsWith("DESCRIBE") ||
                    sql.trim().uppercase().startsWith("PRAGMA")

            if (isSelect) {
                val rs = stmt.executeQuery(sql)
                val metaData = rs.metaData
                val columnCount = metaData.columnCount
                val columns = (1..columnCount).map { metaData.getColumnName(it) }
                val rows = mutableListOf<List<String>>()

                while (rs.next()) {
                    val row = (1..columnCount).map {
                        rs.getString(it) ?: "NULL"
                    }
                    rows.add(row)
                }
                rs.close()
                stmt.close()

                val elapsed = System.currentTimeMillis() - startTime
                DbResult.Success(QueryResult(columns, rows, rows.size, elapsed))
            } else {
                val affected = stmt.executeUpdate(sql)
                stmt.close()
                val elapsed = System.currentTimeMillis() - startTime
                DbResult.Success(
                    QueryResult(
                        columns = listOf("Affected Rows"),
                        rows = listOf(listOf(affected.toString())),
                        rowCount = 1,
                        executionTimeMs = elapsed
                    )
                )
            }
        } catch (e: SQLException) {
            DbResult.Error("SQL 执行错误: ${e.message}")
        } catch (e: Throwable) {
            DbResult.Error("执行异常: ${e.message}")
        }
    }

    suspend fun getTableData(
        tableName: String,
        limit: Int = 100,
        offset: Int = 0
    ): DbResult<QueryResult> = withContext(Dispatchers.IO) {
        val sql = "SELECT * FROM $tableName LIMIT $limit OFFSET $offset"
        executeQuery(sql)
    }

    suspend fun insertRow(
        tableName: String,
        values: Map<String, String>
    ): DbResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val columns = values.keys.joinToString(", ")
            val placeholders = values.keys.joinToString(", ") { "?" }
            val sql = "INSERT INTO $tableName ($columns) VALUES ($placeholders)"

            val conn = connection ?: return@withContext DbResult.Error("未连接数据库")
            val pstmt = conn.prepareStatement(sql)
            values.values.forEachIndexed { index, value ->
                pstmt.setString(index + 1, value)
            }
            pstmt.executeUpdate()
            pstmt.close()
            DbResult.Success(Unit)
        } catch (e: Throwable) {
            DbResult.Error("插入失败: ${e.message}")
        }
    }

    suspend fun updateRow(
        tableName: String,
        values: Map<String, String>,
        whereClause: String
    ): DbResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val setClause = values.keys.joinToString(", ") { "$it = ?" }
            val sql = "UPDATE $tableName SET $setClause WHERE $whereClause"

            val conn = connection ?: return@withContext DbResult.Error("未连接数据库")
            val pstmt = conn.prepareStatement(sql)
            values.values.forEachIndexed { index, value ->
                pstmt.setString(index + 1, value)
            }
            pstmt.executeUpdate()
            pstmt.close()
            DbResult.Success(Unit)
        } catch (e: Throwable) {
            DbResult.Error("更新失败: ${e.message}")
        }
    }

    suspend fun deleteRow(tableName: String, whereClause: String): DbResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val sql = "DELETE FROM $tableName WHERE $whereClause"
            val conn = connection ?: return@withContext DbResult.Error("未连接数据库")
            val stmt = conn.createStatement()
            stmt.executeUpdate(sql)
            stmt.close()
            DbResult.Success(Unit)
        } catch (e: Throwable) {
            DbResult.Error("删除失败: ${e.message}")
        }
    }
}
