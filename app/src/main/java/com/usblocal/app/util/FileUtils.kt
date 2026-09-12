package com.usblocal.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    /** Format bytes to human-readable size */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            .coerceIn(0, units.size - 1)
        val size = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return if (digitGroups == 0) {
            "${bytes} B"
        } else {
            String.format(Locale.getDefault(), "%.1f %s", size, units[digitGroups])
        }
    }

    /** Format epoch millis to readable date */
    fun formatDate(epochMillis: Long): String {
        if (epochMillis <= 0) return ""
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            sdf.format(Date(epochMillis))
        } catch (e: Exception) {
            ""
        }
    }

    /** Get icon emoji for a file extension */
    fun getFileIcon(extension: String, isDirectory: Boolean): String {
        if (isDirectory) return "📁"
        return when (extension.lowercase()) {
            // Images
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "heic", "heif" -> "🖼️"
            // Videos
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp" -> "🎬"
            // Audio
            "mp3", "wav", "flac", "aac", "ogg", "wma", "m4a", "opus" -> "🎵"
            // Documents
            "pdf" -> "📕"
            "doc", "docx" -> "📄"
            "xls", "xlsx" -> "📊"
            "ppt", "pptx" -> "📊"
            "txt", "log", "md", "csv" -> "📝"
            // Code
            "kt", "java", "py", "js", "html", "css", "xml", "json" -> "💻"
            // Archives
            "zip", "rar", "7z", "tar", "gz", "bz2" -> "📦"
            // APK
            "apk" -> "📱"
            // Subtitles
            "srt", "sub", "ass", "ssa", "vtt" -> "💬"
            // ISO
            "iso", "img" -> "💿"
            // Other
            else -> "📄"
        }
    }

    /** Check if extension represents a media file */
    fun isImageFile(extension: String): Boolean =
        extension.lowercase() in setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif")

    fun isVideoFile(extension: String): Boolean =
        extension.lowercase() in setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp")

    fun isAudioFile(extension: String): Boolean =
        extension.lowercase() in setOf("mp3", "wav", "flac", "aac", "ogg", "wma", "m4a", "opus")

    fun isTextFile(extension: String): Boolean =
        extension.lowercase() in setOf("txt", "log", "md", "csv", "json", "xml", "html", "css", "js", "kt", "java", "py")

    fun isPdfFile(extension: String): Boolean =
        extension.lowercase() == "pdf"

    /** Format transfer speed */
    fun formatSpeed(bytesPerSecond: Long): String {
        return "${formatFileSize(bytesPerSecond)}/s"
    }

    /** Estimate remaining time */
    fun formatRemainingTime(remainingBytes: Long, bytesPerSecond: Long): String {
        if (bytesPerSecond <= 0) return "Calculando..."
        val seconds = remainingBytes / bytesPerSecond
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "~${seconds / 60} min"
            else -> "~${seconds / 3600}h ${(seconds % 3600) / 60}m"
        }
    }

    /** Get MIME type from extension for Android intents */
    fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "webm" -> "video/webm"
            "3gp" -> "video/3gpp"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            "aac" -> "audio/aac"
            "ogg" -> "audio/ogg"
            "m4a" -> "audio/mp4"
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "txt", "log", "md" -> "text/plain"
            "csv" -> "text/csv"
            "json" -> "application/json"
            "xml" -> "application/xml"
            "html" -> "text/html"
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            "apk" -> "application/vnd.android.package-archive"
            else -> "application/octet-stream"
        }
    }
}
