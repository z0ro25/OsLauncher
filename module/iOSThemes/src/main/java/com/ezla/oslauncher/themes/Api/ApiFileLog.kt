package com.ezla.oslauncher.themes.Api

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

// Ghi log API ra Download/themes_api_log.txt để đọc được lỗi ở bản release (không có logcat).
// Ghi trên 1 thread riêng theo thứ tự; lỗi ghi file thì bỏ qua, không được làm hỏng luồng gọi API.
object ApiFileLog {
    private const val FILE_NAME = "themes_api_log.txt"
    private const val MAX_BYTES = 2L * 1024 * 1024

    // Che token/chữ ký (header + field JSON chứa token/secret/signature): chỉ giữ 8 ký tự đầu.
    private val SECRET_REGEX = Regex(
        "(?i)(authorization:\\s*(?:bearer\\s+)?|x-signature:\\s*|bearer\\s+)(\\S{0,8})\\S*"
    )
    private val JSON_SECRET_REGEX = Regex(
        "(?i)(\"[a-z_]*(?:token|secret|signature)[a-z_]*\"\\s*:\\s*\")([^\"]{0,8})[^\"]*"
    )

    private val executor = Executors.newSingleThreadExecutor()
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    @Volatile
    private var appContext: Context? = null
    private var uri: Uri? = null

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        val version = runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "${info.versionName} (${info.versionCode})"
        }.getOrDefault("?")
        write("I", "===== app start | v$version | Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) | ${Build.MANUFACTURER} ${Build.MODEL} =====")
    }

    fun write(level: String, message: String) {
        val ctx = appContext ?: return
        val masked = JSON_SECRET_REGEX.replace(SECRET_REGEX.replace(message, "$1$2***"), "$1$2***")
        val line = "${timeFormat.format(Date())} $level $masked\n"
        executor.execute {
            // Lỗi có thể do user xoá file giữa chừng -> quên uri cũ, tạo lại và thử thêm 1 lần.
            runCatching { append(ctx, line) }.onFailure { uri = null; runCatching { append(ctx, line) } }
        }
    }

    private fun append(ctx: Context, line: String) {
        val bytes = line.toByteArray()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val target = uri ?: findOrCreate(ctx)?.also { uri = it } ?: return
            // Quá 2MB thì "wt" ghi đè làm lại từ đầu, còn lại "wa" ghi nối tiếp.
            val mode = if (sizeOf(ctx, target) > MAX_BYTES) "wt" else "wa"
            ctx.contentResolver.openOutputStream(target, mode)?.use { it.write(bytes) }
        } else {
            // Android < 10 cần quyền WRITE_EXTERNAL_STORAGE đã được cấp, chưa cấp thì ghi lỗi -> bỏ qua.
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(dir, FILE_NAME)
            if (file.length() > MAX_BYTES) file.delete()
            file.appendBytes(bytes)
        }
    }

    // Chỉ thấy được file do chính bản cài này tạo; cài lại app thì MediaStore tạo file mới "(1)".
    private fun findOrCreate(ctx: Context): Uri? {
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        ctx.contentResolver.query(
            collection,
            arrayOf(MediaStore.Downloads._ID),
            "${MediaStore.Downloads.DISPLAY_NAME}=? AND ${MediaStore.Downloads.RELATIVE_PATH}=?",
            arrayOf(FILE_NAME, Environment.DIRECTORY_DOWNLOADS + "/"),
            null
        )?.use { c ->
            if (c.moveToFirst()) return Uri.withAppendedPath(collection, c.getLong(0).toString())
        }
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, FILE_NAME)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        return ctx.contentResolver.insert(collection, values)
    }

    private fun sizeOf(ctx: Context, target: Uri): Long = runCatching {
        ctx.contentResolver.openFileDescriptor(target, "r")?.use { it.statSize } ?: 0L
    }.getOrDefault(0L)
}
