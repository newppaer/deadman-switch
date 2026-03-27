package com.example.deadmanswitch.push

import android.util.Log
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * 推送管理器 - 企业微信 / OpenClaw 等 HTTP 推送
 * 所有推送在后台线程执行，不阻塞主线程
 */
object PushManager {
    private const val TAG = "PushManager"

    /**
     * 企业微信群机器人 Webhook 推送
     * POST https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx
     * Body: {"msgtype":"text","text":{"content":"..."}}
     */
    fun sendWeChatWork(webhookUrl: String, message: String) {
        if (webhookUrl.isBlank()) return
        Thread {
            try {
                val url = URL(webhookUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                conn.doOutput = true

                val body = JSONObject().apply {
                    put("msgtype", "text")
                    put("text", JSONObject().apply {
                        put("content", message)
                    })
                }

                OutputStreamWriter(conn.outputStream, "UTF-8").use {
                    it.write(body.toString())
                    it.flush()
                }

                val code = conn.responseCode
                val response = if (code in 200..299) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    conn.errorStream?.bufferedReader()?.readText() ?: "no body"
                }
                Log.d(TAG, "WeChatWork response: $code $response")
                conn.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "WeChatWork push failed", e)
            }
        }.start()
    }

    /**
     * 通用 HTTP POST 推送（用于 OpenClaw API 等）
     * POST 到用户配置的 URL
     * Body: {"title":"...","content":"...","timestamp":...}
     */
    fun sendHttpPost(apiUrl: String, token: String, title: String, content: String) {
        if (apiUrl.isBlank()) return
        Thread {
            try {
                val url = URL(apiUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                if (token.isNotBlank()) {
                    conn.setRequestProperty("Authorization", "Bearer $token")
                }
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                conn.doOutput = true

                val body = JSONObject().apply {
                    put("title", title)
                    put("content", content)
                    put("timestamp", System.currentTimeMillis())
                    put("source", "DeadManSwitch")
                }

                OutputStreamWriter(conn.outputStream, "UTF-8").use {
                    it.write(body.toString())
                    it.flush()
                }

                val code = conn.responseCode
                val response = if (code in 200..299) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    conn.errorStream?.bufferedReader()?.readText() ?: "no body"
                }
                Log.d(TAG, "HttpPost[$apiUrl] response: $code $response")
                conn.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "HttpPost[$apiUrl] failed", e)
            }
        }.start()
    }
}
