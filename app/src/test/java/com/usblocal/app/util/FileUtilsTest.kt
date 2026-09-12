package com.usblocal.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileUtilsTest {

    @Test
    fun `formatFileSize formats bytes correctly`() {
        assertEquals("0 B", FileUtils.formatFileSize(0))
        assertEquals("512 B", FileUtils.formatFileSize(512))
        assertEquals("1.0 KB", FileUtils.formatFileSize(1024))
        assertEquals("1.5 KB", FileUtils.formatFileSize(1536))
        assertEquals("1.0 MB", FileUtils.formatFileSize(1_048_576))
        assertEquals("2.5 GB", FileUtils.formatFileSize(2_684_354_560))
        assertEquals("1.0 TB", FileUtils.formatFileSize(1_099_511_627_776))
    }

    @Test
    fun `getFileIcon returns correct icons`() {
        assertEquals("📁", FileUtils.getFileIcon("", true))
        assertEquals("🖼️", FileUtils.getFileIcon("jpg", false))
        assertEquals("🖼️", FileUtils.getFileIcon("PNG", false))
        assertEquals("🎬", FileUtils.getFileIcon("mp4", false))
        assertEquals("🎬", FileUtils.getFileIcon("mkv", false))
        assertEquals("🎵", FileUtils.getFileIcon("mp3", false))
        assertEquals("📕", FileUtils.getFileIcon("pdf", false))
        assertEquals("📝", FileUtils.getFileIcon("txt", false))
        assertEquals("📦", FileUtils.getFileIcon("zip", false))
        assertEquals("📄", FileUtils.getFileIcon("xyz", false))
    }

    @Test
    fun `isImageFile detects image extensions`() {
        assertTrue(FileUtils.isImageFile("jpg"))
        assertTrue(FileUtils.isImageFile("PNG"))
        assertTrue(FileUtils.isImageFile("webp"))
        assertFalse(FileUtils.isImageFile("mp4"))
        assertFalse(FileUtils.isImageFile("txt"))
    }

    @Test
    fun `isVideoFile detects video extensions`() {
        assertTrue(FileUtils.isVideoFile("mp4"))
        assertTrue(FileUtils.isVideoFile("MKV"))
        assertTrue(FileUtils.isVideoFile("avi"))
        assertFalse(FileUtils.isVideoFile("jpg"))
        assertFalse(FileUtils.isVideoFile("mp3"))
    }

    @Test
    fun `getMimeType returns correct MIME types`() {
        assertEquals("image/jpeg", FileUtils.getMimeType("jpg"))
        assertEquals("video/mp4", FileUtils.getMimeType("mp4"))
        assertEquals("audio/mpeg", FileUtils.getMimeType("mp3"))
        assertEquals("application/pdf", FileUtils.getMimeType("pdf"))
        assertEquals("text/plain", FileUtils.getMimeType("txt"))
        assertEquals("application/octet-stream", FileUtils.getMimeType("unknown"))
    }

    @Test
    fun `formatSpeed formats correctly`() {
        assertEquals("1.0 KB/s", FileUtils.formatSpeed(1024))
        assertEquals("10.0 MB/s", FileUtils.formatSpeed(10_485_760))
    }

    @Test
    fun `formatRemainingTime handles edge cases`() {
        assertEquals("Calculando...", FileUtils.formatRemainingTime(1000, 0))
        assertEquals("30s", FileUtils.formatRemainingTime(30_000, 1000))
        assertEquals("~5 min", FileUtils.formatRemainingTime(300_000, 1000))
    }
}
