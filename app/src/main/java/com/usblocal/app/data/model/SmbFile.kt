package com.usblocal.app.data.model

data class SmbFile(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0L,
    val lastModified: Long = 0L,
    val extension: String = ""
) {
    companion object {
        fun fromDirectoryInfo(
            fileName: String,
            parentPath: String,
            isDirectory: Boolean,
            fileSize: Long,
            lastWriteTime: Long
        ): SmbFile {
            val cleanParent = parentPath.trimEnd('\\', '/')
            val fullPath = if (cleanParent.isEmpty()) fileName else "$cleanParent\\$fileName"
            val ext = if (!isDirectory && fileName.contains('.')) {
                fileName.substringAfterLast('.').lowercase()
            } else ""

            return SmbFile(
                name = fileName,
                path = fullPath,
                isDirectory = isDirectory,
                size = fileSize,
                lastModified = lastWriteTime,
                extension = ext
            )
        }
    }
}
