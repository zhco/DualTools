package com.dualtools

import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.dualtools.ui.screens.HomeScreen
import com.dualtools.ui.theme.DualToolsTheme
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Global crash handler - captures ALL uncaught exceptions including native crashes
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            writeCrashLog(throwable)
            defaultHandler?.uncaughtException(thread, throwable)
            // Force kill after native crash to avoid corrupted state
            android.os.Process.killProcess(android.os.Process.myPid())
        }

        setContent {
            DualToolsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen()
                }
            }
        }
    }

    private fun writeCrashLog(throwable: Throwable) {
        try {
            val crashDir = File(getExternalFilesDir(null), "crashes")
            crashDir.mkdirs()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val crashFile = File(crashDir, "crash_$timestamp.txt")

            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            pw.flush()

            crashFile.writeText("Time: $timestamp\nThread: ${Thread.currentThread().name}\n\n$sw")
        } catch (_: Throwable) {}
    }
}