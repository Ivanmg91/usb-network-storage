package com.usblocal.app.data.repository

import com.usblocal.app.data.model.SmbConnection
import com.usblocal.app.data.model.SmbFile
import com.usblocal.app.data.smb.SmbDataSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepository @Inject constructor(
    private val smbDataSource: SmbDataSource
) {
    // Simple metadata cache with TTL
    private data class CacheEntry(
        val files: List<SmbFile>,
        val timestamp: Long
    )

    private val cache = mutableMapOf<String, CacheEntry>()
    private val cacheMutex = Mutex()

    suspend fun listFiles(
        connection: SmbConnection,
        path: String,
        forceRefresh: Boolean = false
    ): Result<List<SmbFile>> {
        val cacheKey = "${connection.id}:$path"

        if (!forceRefresh) {
            cacheMutex.withLock {
                cache[cacheKey]?.let { entry ->
                    if (System.currentTimeMillis() - entry.timestamp < CACHE_TTL_MS) {
                        return Result.success(entry.files)
                    }
                }
            }
        }

        val result = smbDataSource.listFiles(connection, path)

        if (result.isSuccess) {
            cacheMutex.withLock {
                cache[cacheKey] = CacheEntry(result.getOrThrow(), System.currentTimeMillis())
            }
        }

        return result
    }

    fun invalidateCache(connection: SmbConnection, path: String) {
        val cacheKey = "${connection.id}:$path"
        cache.remove(cacheKey)
    }

    fun invalidateAllCache() {
        cache.clear()
    }

    suspend fun createFolder(
        connection: SmbConnection,
        path: String,
        name: String
    ): Result<Unit> {
        val result = smbDataSource.createFolder(connection, path, name)
        if (result.isSuccess) invalidateCache(connection, path)
        return result
    }

    suspend fun delete(
        connection: SmbConnection,
        path: String,
        isDirectory: Boolean
    ): Result<Unit> {
        val parentPath = path.substringBeforeLast('\\', "").substringBeforeLast('/', "")
        val result = smbDataSource.delete(connection, path, isDirectory)
        if (result.isSuccess) invalidateCache(connection, parentPath)
        return result
    }

    suspend fun rename(
        connection: SmbConnection,
        path: String,
        newName: String
    ): Result<Unit> {
        val parentPath = path.substringBeforeLast('\\', "").substringBeforeLast('/', "")
        val result = smbDataSource.rename(connection, path, newName)
        if (result.isSuccess) invalidateCache(connection, parentPath)
        return result
    }

    suspend fun downloadFile(
        connection: SmbConnection,
        remotePath: String,
        outputStream: OutputStream,
        onProgress: ((Long, Long) -> Unit)? = null
    ): Result<Unit> {
        return smbDataSource.downloadFile(connection, remotePath, outputStream, onProgress)
    }

    suspend fun getStorageInfo(connection: SmbConnection, path: String): Result<Pair<Long, Long>> {
        return smbDataSource.getStorageInfo(connection, path)
    }

    suspend fun uploadFile(
        connection: SmbConnection,
        remotePath: String,
        inputStream: InputStream,
        totalSize: Long,
        onProgress: ((Long, Long) -> Unit)? = null
    ): Result<Unit> {
        val parentPath = remotePath.substringBeforeLast('\\', "").substringBeforeLast('/', "")
        val result = smbDataSource.uploadFile(connection, remotePath, inputStream, totalSize, onProgress)
        if (result.isSuccess) invalidateCache(connection, parentPath)
        return result
    }

    fun closeAll() {
        smbDataSource.closeAll()
        cache.clear()
    }

    companion object {
        private const val CACHE_TTL_MS = 30_000L // 30 seconds
    }
}
