package com.example.deadmanswitch

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

object CrashLogger {
    private const val FILE_NAME = "crash_log.txt"

    fun install(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                pw.flush()

                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val log = buildString {
                    appendLine("=== Crash Report ===")
                    appendLine("Time: ${sdf.format(Date())}")
                    appendLine("Thread: ${thread.name}")
                    appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                    appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                    appendLine("App: v2.0")
                    appendLine("=== Stack Trace ===")
                    appendLine(sw.toString())
                    appendLine("=== End ===")
                    appendLine()
                }

                // 追加写入文件
                val file = File(context.filesDir, FILE_NAME)
                file.appendText(log)
            } catch (_: Exception) {
                // 忽略日志写入失败
            }

            // 交给默认处理器
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun readLog(context: Context): String {
        val file = File(context.filesDir, FILE_NAME)
        return if (file.exists()) file.readText() else ""
    }

    fun clearLog(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        if (file.exists()) file.delete()
    }

    fun hasLog(context: Context): Boolean {
        val file = File(context.filesDir, FILE_NAME)
        return file.exists() && file.length() > 0
    }
}
