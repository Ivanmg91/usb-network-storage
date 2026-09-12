package com.usblocal.app.data.smb

import com.usblocal.app.data.model.ConnectionTestResult
import com.usblocal.app.data.model.SmbConnection
import com.usblocal.app.data.model.SmbFile
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream

/**
 * Abstract interface for SMB operations.
 * Can be replaced with a fake implementation for testing.
 */
interface SmbDataSource {

    /** Test connection step-by-step, emitting progress */
    suspend fun testConnection(connection: SmbConnection): Flow<ConnectionTestResult>

    /** List files in a directory on the SMB share */
    suspend fun listFiles(connection: SmbConnection, path: String): Result<List<SmbFile>>

    /** Create a new folder */
    suspend fun createFolder(connection: SmbConnection, path: String, name: String): Result<Unit>

    /** Delete a file or folder */
    suspend fun delete(connection: SmbConnection, path: String, isDirectory: Boolean): Result<Unit>

    /** Rename a file or folder */
    suspend fun rename(connection: SmbConnection, path: String, newName: String): Result<Unit>

    /** Download a file from SMB to a local output stream */
    suspend fun downloadFile(
        connection: SmbConnection,
        remotePath: String,
        outputStream: OutputStream,
        onProgress: ((bytesTransferred: Long, totalBytes: Long) -> Unit)? = null
    ): Result<Unit>

    /** Upload a local file to SMB */
    suspend fun uploadFile(
        connection: SmbConnection,
        remotePath: String,
        inputStream: InputStream,
        totalSize: Long,
        onProgress: ((bytesTransferred: Long, totalBytes: Long) -> Unit)? = null
    ): Result<Unit>

    /** Retrieve free and total space in bytes */
    suspend fun getStorageInfo(connection: SmbConnection, path: String): Result<Pair<Long, Long>>

    /** Close all active connections and clean up */
    fun closeAll()
}
