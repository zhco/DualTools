# Apache Commons Net
-keep class org.apache.commons.net.** { *; }
-dontwarn org.apache.commons.net.**

# JDBC Drivers
-keep class com.mysql.jdbc.** { *; }
-dontwarn com.mysql.jdbc.**
-keep class org.postgresql.** { *; }
-dontwarn org.postgresql.**
-keep class org.sqlite.** { *; }
-dontwarn org.sqlite.**
-keep class com.microsoft.sqlserver.** { *; }
-dontwarn com.microsoft.sqlserver.**
-keep class oracle.jdbc.** { *; }
-dontwarn oracle.jdbc.**

# JDBC Driver dependencies
-dontwarn com.sun.jna.**
-dontwarn org.antlr.v4.**
-dontwarn com.zaxxer.hikari.**
-dontwarn javax.naming.**
-dontwarn javax.management.**
-dontwarn oracle.net.**
-dontwarn oracle.security.**

# Keep data classes
-keep class com.dualtools.ftp.FtpFileItem { *; }
-keep class com.dualtools.ftp.FtpConnection { *; }
-keep class com.dualtools.database.DbConnection { *; }
-keep class com.dualtools.database.TableInfo { *; }
-keep class com.dualtools.database.ColumnInfo { *; }
-keep class com.dualtools.database.QueryResult { *; }
