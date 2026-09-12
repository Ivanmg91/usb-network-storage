package com.usblocal.app.data.smb

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mserref.NtStatus
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2CreateOptions
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.mssmb2.SMBApiException
import com.hierynomus.protocol.transport.TransportException
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File as SmbFile2
import com.usblocal.app.data.model.ConnectionTestResult
import com.usblocal.app.data.model.SmbConnection
import com.usblocal.app.data.model.SmbError
import com.usblocal.app.data.model.SmbFile
import com.usblocal.app.data.model.StepResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.UnknownHostException
import java.util.EnumSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmbDataSourceImpl @Inject constructor() : SmbDataSource {

    private val smbConfig: SmbConfig = SmbConfig.builder()
        .withTimeout(30, TimeUnit.SECONDS)
        .withSoTimeout(30, TimeUnit.SECONDS)
        .withMultiProtocolNegotiate(true)
        .build()

    private val client: SMBClient = SMBClient(smbConfig)

    private data class ShareKey(val connectionId: String)
    private data class ShareSession(
        val connection: Connection,
        val session: Session,
        val share: DiskShare
    )

    private val activeShares = ConcurrentHashMap<ShareKey, ShareSession>()

    // ==================== Test Connection ====================

    override suspend fun testConnection(connection: SmbConnection): Flow<ConnectionTestResult> = flow {
        var result = ConnectionTestResult()

        // Step 1: Host + port reachable
        result = result.copy(hostReachable = StepResult.InProgress)
        emit(result)
        try {
            withContext(Dispatchers.IO) {
                Socket().use { it.connect(InetSocketAddress(connection.host, connection.port), 5000) }
            }
            result = result.copy(hostReachable = StepResult.Success, portAccessible = StepResult.Success)
            emit(result)
        } catch (e: UnknownHostException) {
            result = result.copy(hostReachable = StepResult.Failed(SmbError.HostUnreachable(connection.host)))
            emit(result); return@flow
        } catch (e: Exception) {
            val error = if (e.message?.contains("refused", true) == true) {
                result = result.copy(hostReachable = StepResult.Success)
                SmbError.PortInaccessible(connection.host, connection.port)
            } else {
                SmbError.HostUnreachable(connection.host)
            }
            result = result.copy(
                portAccessible = StepResult.Failed(error).takeIf { result.hostReachable is StepResult.Success }
                    ?: result.portAccessible,
                hostReachable = if (result.hostReachable !is StepResult.Success) StepResult.Failed(error) else result.hostReachable
            )
            emit(result); return@flow
        }

        // Step 3: SMB negotiation
        result = result.copy(smbNegotiated = StepResult.InProgress)
        emit(result)
        val conn: Connection
        try {
            conn = withContext(Dispatchers.IO) { client.connect(connection.host, connection.port) }
            result = result.copy(smbNegotiated = StepResult.Success)
            emit(result)
        } catch (e: Exception) {
            result = result.copy(smbNegotiated = StepResult.Failed(SmbError.NegotiationError()))
            emit(result); return@flow
        }

        // Step 4: Authentication
        result = result.copy(authenticated = StepResult.InProgress)
        emit(result)
        val session: Session
        try {
            session = withContext(Dispatchers.IO) { conn.authenticate(buildAuthContext(connection)) }
            result = result.copy(authenticated = StepResult.Success)
            emit(result)
        } catch (e: SMBApiException) {
            result = result.copy(authenticated = StepResult.Failed(mapSmbApiError(e)))
            emit(result); safeClose(conn); return@flow
        } catch (e: Exception) {
            result = result.copy(authenticated = StepResult.Failed(SmbError.AuthenticationFailed()))
            emit(result); safeClose(conn); return@flow
        }

        // Step 5: Share accessible
        result = result.copy(shareAccessible = StepResult.InProgress)
        emit(result)
        val share: DiskShare
        try {
            share = withContext(Dispatchers.IO) { session.connectShare(connection.shareName) as DiskShare }
            result = result.copy(shareAccessible = StepResult.Success)
            emit(result)
        } catch (e: SMBApiException) {
            result = result.copy(shareAccessible = StepResult.Failed(mapSmbApiError(e, connection.shareName)))
            emit(result); safeClose(conn); return@flow
        } catch (e: Exception) {
            result = result.copy(shareAccessible = StepResult.Failed(SmbError.ShareNotFound(connection.shareName)))
            emit(result); safeClose(conn); return@flow
        }

        // Step 6: Can list
        result = result.copy(canListFiles = StepResult.InProgress)
        emit(result)
        try {
            withContext(Dispatchers.IO) { share.list("") }
            result = result.copy(canListFiles = StepResult.Success)
            emit(result)
        } catch (e: Exception) {
            result = result.copy(canListFiles = StepResult.Failed(SmbError.AccessDenied(connection.shareName)))
            emit(result)
        }
        safeClose(share); safeClose(conn)
    }

    // ==================== File Operations ====================

    override suspend fun listFiles(
        connection: SmbConnection, path: String
    ): Result<List<SmbFile>> = withContext(Dispatchers.IO) {
        runCatchingSmbOp(connection) {
            val share = getOrCreateShare(connection)
            val smbPath = normalizePath(path)
            share.list(smbPath)
                .filter { it.fileName != "." && it.fileName != ".." }
                .map { info ->
                    val isDir = (info.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L
                    SmbFile.fromDirectoryInfo(
                        fileName = info.fileName,
                        parentPath = smbPath,
                        isDirectory = isDir,
                        fileSize = if (isDir) 0L else info.endOfFile,
                        lastWriteTime = fileTimeToMillis(info.lastWriteTime?.windowsTimeStamp ?: 0L)
                    )
                }
                .sortedWith(compareByDescending<SmbFile> { it.isDirectory }.thenBy { it.name.lowercase() })
        }
    }

    override suspend fun createFolder(
        connection: SmbConnection, path: String, name: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatchingSmbOp(connection) {
            val share = getOrCreateShare(connection)
            val parent = normalizePath(path)
            share.mkdir(if (parent.isEmpty()) name else "$parent\\$name")
        }
    }

    override suspend fun delete(
        connection: SmbConnection, path: String, isDirectory: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatchingSmbOp(connection) {
            val share = getOrCreateShare(connection)
            val smbPath = normalizePath(path)
            if (isDirectory) share.rmdir(smbPath, true) else share.rm(smbPath)
        }
    }

    override suspend fun rename(
        connection: SmbConnection, path: String, newName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatchingSmbOp(connection) {
            val share = getOrCreateShare(connection)
            val smbPath = normalizePath(path)
            val parentDir = smbPath.substringBeforeLast('\\', "")
            val newPath = if (parentDir.isEmpty()) newName else "$parentDir\\$newName"

            // Use DiskShare.open() with DELETE access to get a DiskEntry, then rename
            val entry = share.open(
                smbPath,
                EnumSet.of(AccessMask.DELETE, AccessMask.GENERIC_READ),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null
            )
            try {
                entry.rename(newPath)
            } finally {
                entry.close()
            }
        }
    }

    override suspend fun downloadFile(
        connection: SmbConnection,
        remotePath: String,
        outputStream: OutputStream,
        onProgress: ((Long, Long) -> Unit)?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatchingSmbOp(connection) {
            val share = getOrCreateShare(connection)
            val smbPath = normalizePath(remotePath)

            val smbFile = share.open(
                smbPath,
                EnumSet.of(AccessMask.GENERIC_READ),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null
            ) as SmbFile2

            try {
                val totalSize = smbFile.fileInformation.standardInformation.endOfFile
                val inStream = smbFile.inputStream
                transferStream(inStream, outputStream, totalSize, onProgress)
            } finally {
                smbFile.close()
            }
        }
    }

    override suspend fun uploadFile(
        connection: SmbConnection,
        remotePath: String,
        inputStream: InputStream,
        totalSize: Long,
        onProgress: ((Long, Long) -> Unit)?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatchingSmbOp(connection) {
            val share = getOrCreateShare(connection)
            val smbPath = normalizePath(remotePath)

            val smbFile = share.open(
                smbPath,
                EnumSet.of(AccessMask.GENERIC_WRITE),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OVERWRITE_IF,
                null
            ) as SmbFile2

            try {
                val outStream = smbFile.outputStream
                transferStream(inputStream, outStream, totalSize, onProgress)
            } finally {
                smbFile.close()
            }
        }
    }

    override fun closeAll() {
        activeShares.values.forEach {
            safeClose(it.share); safeClose(it.connection)
        }
        activeShares.clear()
    }

    // ==================== Internals ====================

    private fun getOrCreateShare(connection: SmbConnection): DiskShare {
        val key = ShareKey(connection.id)
        activeShares[key]?.let { cached ->
            if (cached.connection.isConnected && cached.share.isConnected) return cached.share
            safeClose(cached.share); safeClose(cached.connection)
            activeShares.remove(key)
        }
        val conn = client.connect(connection.host, connection.port)
        val session = conn.authenticate(buildAuthContext(connection))
        val share = session.connectShare(connection.shareName) as DiskShare
        activeShares[key] = ShareSession(conn, session, share)
        return share
    }

    private fun invalidateShare(connection: SmbConnection) {
        activeShares.remove(ShareKey(connection.id))?.let {
            safeClose(it.share); safeClose(it.connection)
        }
    }

    private fun buildAuthContext(conn: SmbConnection): AuthenticationContext =
        if (conn.isGuest) AuthenticationContext.guest()
        else AuthenticationContext(conn.username, conn.password.toCharArray(), conn.domain.ifBlank { null })

    private inline fun <T> runCatchingSmbOp(connection: SmbConnection, block: () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: SMBApiException) {
            invalidateShare(connection)
            Result.failure(SmbException(mapSmbApiError(e)))
        } catch (e: TransportException) {
            invalidateShare(connection)
            Result.failure(SmbException(SmbError.ConnectionLost()))
        } catch (e: UnknownHostException) {
            invalidateShare(connection)
            Result.failure(SmbException(SmbError.HostUnreachable(connection.host)))
        } catch (e: IOException) {
            invalidateShare(connection)
            val error = when {
                e.message?.contains("timeout", true) == true -> SmbError.Timeout()
                e.message?.contains("refused", true) == true -> SmbError.PortInaccessible(connection.host, connection.port)
                else -> SmbError.ConnectionLost()
            }
            Result.failure(SmbException(error))
        } catch (e: Exception) {
            invalidateShare(connection)
            Result.failure(SmbException(SmbError.Unknown(e)))
        }
    }

    private fun mapSmbApiError(e: SMBApiException, context: String = ""): SmbError = when (e.status) {
        NtStatus.STATUS_LOGON_FAILURE -> SmbError.AuthenticationFailed()
        NtStatus.STATUS_ACCESS_DENIED -> SmbError.AccessDenied(context)
        NtStatus.STATUS_BAD_NETWORK_NAME -> SmbError.ShareNotFound(context)
        NtStatus.STATUS_OBJECT_NAME_NOT_FOUND -> SmbError.FileNotFound(context)
        NtStatus.STATUS_OBJECT_NAME_COLLISION -> SmbError.FileAlreadyExists(context)
        NtStatus.STATUS_DISK_FULL -> SmbError.InsufficientStorage()
        NtStatus.STATUS_NETWORK_NAME_DELETED -> SmbError.ConnectionLost()
        else -> SmbError.Unknown(e)
    }

    private fun transferStream(input: InputStream, output: OutputStream, total: Long, onProgress: ((Long, Long) -> Unit)?) {
        val buf = ByteArray(BUFFER_SIZE)
        var transferred = 0L
        var n: Int
        while (input.read(buf).also { n = it } != -1) {
            output.write(buf, 0, n)
            transferred += n
            onProgress?.invoke(transferred, total)
        }
        output.flush()
    }

    private fun normalizePath(path: String): String = path.replace('/', '\\').trimStart('\\')

    private fun fileTimeToMillis(ts: Long): Long =
        if (ts <= 0L) 0L else (ts / 10000) - 11644473600000L

    private fun safeClose(c: AutoCloseable?) { try { c?.close() } catch (_: Exception) {} }

    companion object { private const val BUFFER_SIZE = 65536 }
}

/** Wrapper exception carrying a typed [SmbError] */
class SmbException(val smbError: SmbError) : Exception(smbError.userMessage)
