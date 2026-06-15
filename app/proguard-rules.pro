# Apache Commons Net
-keep class org.apache.commons.net.** { *; }
-dontwarn org.apache.commons.net.**

# JDBC Drivers
-keep class com.mysql.jdbc.** { *; }
-keep class org.postgresql.** { *; }
-keep class org.sqlite.** { *; }
-keep class com.microsoft.sqlserver.** { *; }
-keep class oracle.jdbc.** { *; }

# Keep data classes
-keep class com.dualtools.ftp.FtpFileItem { *; }
-keep class com.dualtools.ftp.FtpConnection { *; }
-keep class com.dualtools.database.DbConnection { *; }
-keep class com.dualtools.database.TableInfo { *; }
-keep class com.dualtools.database.ColumnInfo { *; }
-keep class com.dualtools.database.QueryResult { *; }
