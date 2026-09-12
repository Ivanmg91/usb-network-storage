package com.usblocal.app.data.smb

import com.usblocal.app.data.model.ConnectionTestResult
import com.usblocal.app.data.model.SmbConnection
import com.usblocal.app.data.model.SmbError
import com.usblocal.app.data.model.SmbFile
import com.usblocal.app.data.model.StepResult
import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbException as JcifsSmbException
import jcifs.smb.SmbAuthException
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
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmbDataSourceImpl @Inject constructor() : SmbDataSource {

    private val baseContext: CIFSContext by lazy {
        val prop = Properties()
        // Habilitar SMB1 y SMB2/3 para máxima compatibilidad con routers antiguos y modernos
        prop.setProperty("jcifs.smb.client.enableSMB2", "true")
        prop.setProperty("jcifs.smb.client.disableSMB1", "false")
        prop.setProperty("jcifs.resolveOrder", "DNS")
        prop.setProperty("jcifs.smb.client.ipcSigningEnforced", "false") // Compatibility for older routers
        val config = PropertyConfiguration(prop)
        BaseContext(config)
    }

    private val contextCache = ConcurrentHashMap<String, CIFSContext>()

    private fun getContext(connection: SmbConnection): CIFSContext {
        return contextCache.getOrPut(connection.id) {
            val auth = if (connection.isGuest) {
                NtlmPasswordAuthenticator("", "GUEST", "")
            } else {
                NtlmPasswordAuthenticator(
                    connection.domain.ifEmpty { null },
                    connection.username,
                    connection.password
                )
            }
            baseContext.withCredentials(auth)
        }
    }

    private fun buildUri(connection: SmbConnection, path: String, isDirectory: Boolean): String {
        val normalizedPath = path.replace('\\', '/').trim('/')
        val shareName = connection.shareName.trim('/')
        
        val uri = StringBuilder("smb://${connection.host}/")
        if (shareName.isNotEmpty()) {
            uri.append(shareName).append("/")
        }
        if (normalizedPath.isNotEmpty()) {
            uri.append(normalizedPath)
            if (isDirectory) {
                uri.append("/")
            }
        }
        return uri.toString()
    }

    // ==================== Test Connection ====================

    override suspend fun testConnection(connection: SmbConnection): Flow<ConnectionTestResult> = flow {
        var result = ConnectionTestResult()
        val context = try {
            withContext(Dispatchers.IO) { getContext(connection) }
        } catch (e: Throwable) {
            result = result.copy(smbNegotiated = StepResult.Failed(SmbError.Unknown(Exception("Error de librería SMB: ${e.message}"))))
            emit(result); return@flow
        }

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

        // Step 2, 3, 4, 5 combined in jcifs (it negotiates and authenticates lazily when opening)
        result = result.copy(smbNegotiated = StepResult.InProgress)
        emit(result)
        
        try {
            val shareUri = buildUri(connection, "", true)
            val smbFile = withContext(Dispatchers.IO) { jcifs.smb.SmbFile(shareUri, context) }
            
            // Trigger connection/auth by checking existence
            withContext(Dispatchers.IO) { smbFile.exists() }
            
            result = result.copy(
                smbNegotiated = StepResult.Success,
                authenticated = StepResult.Success,
                shareAccessible = StepResult.Success
            )
            emit(result)
            
            // Step 6: Can list
            result = result.copy(canListFiles = StepResult.InProgress)
            emit(result)
            
            withContext(Dispatchers.IO) { smbFile.listFiles() }
            result = result.copy(canListFiles = StepResult.Success)
            emit(result)

        } catch (e: SmbAuthException) {
            result = result.copy(
                smbNegotiated = StepResult.Success,
                authenticated = StepResult.Failed(SmbError.AuthenticationFailed())
            )
            emit(result)
        } catch (e: JcifsSmbException) {
            val error = mapJcifsError(e, connection.shareName)
            // It could be negotiation, share not found, etc.
            result = result.copy(
                smbNegotiated = StepResult.Success, // We assume negotiation succeeded if it's an SmbException not related to transport
                authenticated = StepResult.Success,
                shareAccessible = StepResult.Failed(error)
            )
            emit(result)
        } catch (e: Throwable) {
            result = result.copy(smbNegotiated = StepResult.Failed(SmbError.Unknown(Exception(e.message))))
            emit(result)
        }
    }

    // ==================== File Operations ====================

    override suspend fun listFiles(
        connection: SmbConnection, path: String
    ): Result<List<SmbFile>> = withContext(Dispatchers.IO) {
        runCatchingSmbOp {
            val uri = buildUri(connection, path, true)
            val smbFile = jcifs.smb.SmbFile(uri, getContext(connection))
            
            val children = smbFile.listFiles() ?: emptyArray()
            
            children.map { child ->
                val isDir = child.isDirectory
                val name = child.name.trimEnd('/')
                val childPath = if (path.isEmpty()) name else "$path\\$name"
                
                SmbFile(
                    name = name,
                    path = childPath,
                    isDirectory = isDir,
                    size = if (isDir) 0L else child.length(),
                    lastModified = child.lastModified()
                )
            }.sortedWith(compareByDescending<SmbFile> { it.isDirectory }.thenBy { it.name.lowercase() })
        }
    }

    override suspend fun createFolder(
        connection: SmbConnection, path: String, name: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatchingSmbOp {
            val parentPath = if (path.isEmpty()) name else "$path\\$name"
            val uri = buildUri(connection, parentPath, true)
            val smbFile = jcifs.smb.SmbFile(uri, getContext(connection))
            smbFile.mkdir()
        }
    }

    override suspend fun delete(
        connection: SmbConnection, path: String, isDirectory: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatchingSmbOp {
            val uri = buildUri(connection, path, isDirectory)
            val smbFile = jcifs.smb.SmbFile(uri, getContext(connection))
            smbFile.delete()
        }
    }

    override suspend fun rename(
        connection: SmbConnection, path: String, newName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatchingSmbOp {
            val parentDir = path.substringBeforeLast('\\', "")
            val newPath = if (parentDir.isEmpty()) newName else "$parentDir\\$newName"
            
            // Jcifs requires knowing if it's a directory to append slash, 
            // but we can try without slash for rename source and destination
            val oldUri = buildUri(connection, path, false) 
            val newUri = buildUri(connection, newPath, false)
            
            val oldFile = jcifs.smb.SmbFile(oldUri, getContext(connection))
            val newFile = jcifs.smb.SmbFile(newUri, getContext(connection))
            
            oldFile.renameTo(newFile)
        }
    }

    override suspend fun downloadFile(
        connection: SmbConnection,
        remotePath: String,
        outputStream: OutputStream,
        onProgress: ((Long, Long) -> Unit)?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatchingSmbOp {
            val uri = buildUri(connection, remotePath, false)
            val smbFile = jcifs.smb.SmbFile(uri, getContext(connection))
            
            val totalSize = smbFile.length()
            smbFile.inputStream.use { inStream ->
                transferStream(inStream, outputStream, totalSize, onProgress)
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
        runCatchingSmbOp {
            val uri = buildUri(connection, remotePath, false)
            val smbFile = jcifs.smb.SmbFile(uri, getContext(connection))
            
            smbFile.outputStream.use { outStream ->
                transferStream(inputStream, outStream, totalSize, onProgress)
            }
        }
    }

    override suspend fun getStorageInfo(connection: SmbConnection, path: String): Result<Pair<Long, Long>> = withContext(Dispatchers.IO) {
        runCatchingSmbOp {
            val uri = buildUri(connection, path, true)
            val smbFile = jcifs.smb.SmbFile(uri, getContext(connection))
            val free = smbFile.diskFreeSpace
            Pair(free, 0L)
        }
    }

    override fun closeAll() {
        contextCache.clear()
        // jcifs-ng handles connection pooling internally via BaseContext
    }

    // ==================== Internals ====================

    private inline fun <T> runCatchingSmbOp(block: () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: SmbAuthException) {
            Result.failure(SmbException(SmbError.AuthenticationFailed()))
        } catch (e: JcifsSmbException) {
            Result.failure(SmbException(mapJcifsError(e)))
        } catch (e: UnknownHostException) {
            Result.failure(SmbException(SmbError.HostUnreachable("")))
        } catch (e: IOException) {
            Result.failure(SmbException(SmbError.ConnectionLost()))
        } catch (e: Throwable) {
            Result.failure(SmbException(SmbError.Unknown(Exception(e.message))))
        }
    }

    private fun mapJcifsError(e: JcifsSmbException, context: String = ""): SmbError {
        val ntStatus = e.ntStatus
        return when (ntStatus) {
            jcifs.smb.NtStatus.NT_STATUS_ACCESS_DENIED -> SmbError.AccessDenied(context)
            jcifs.smb.NtStatus.NT_STATUS_BAD_NETWORK_NAME -> SmbError.ShareNotFound(context)
            jcifs.smb.NtStatus.NT_STATUS_OBJECT_NAME_NOT_FOUND, 
            jcifs.smb.NtStatus.NT_STATUS_NO_SUCH_FILE -> SmbError.FileNotFound(context)
            jcifs.smb.NtStatus.NT_STATUS_OBJECT_NAME_COLLISION -> SmbError.FileAlreadyExists(context)
            jcifs.smb.NtStatus.NT_STATUS_DISK_FULL -> SmbError.InsufficientStorage()
            else -> SmbError.Unknown(e)
        }
    }

    private fun transferStream(input: InputStream, output: OutputStream, total: Long, onProgress: ((Long, Long) -> Unit)?) {
        val buf = ByteArray(65536)
        var transferred = 0L
        var n: Int
        while (input.read(buf).also { n = it } != -1) {
            output.write(buf, 0, n)
            transferred += n
            onProgress?.invoke(transferred, total)
        }
        output.flush()
    }
}

/** Wrapper exception carrying a typed [SmbError] */
class SmbException(val smbError: SmbError) : Exception(smbError.userMessage)
