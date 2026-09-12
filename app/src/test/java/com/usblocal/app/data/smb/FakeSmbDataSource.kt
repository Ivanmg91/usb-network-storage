package com.usblocal.app.data.smb

import com.usblocal.app.data.model.ConnectionTestResult
import com.usblocal.app.data.model.SmbConnection
import com.usblocal.app.data.model.SmbFile
import com.usblocal.app.data.model.StepResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.InputStream
import java.io.OutputStream

/**
 * Fake implementation of [SmbDataSource] for testing without a real SMB server.
 */
class FakeSmbDataSource : SmbDataSource {

    var shouldFail = false
    var failError: Exception = RuntimeException("Fake error")

    // Simulated file system
    private val fileSystem = mutableMapOf<String, MutableList<SmbFile>>()

    init {
        // Default test data
        fileSystem[""] = mutableListOf(
            SmbFile("Películas", "Películas", true, 0, System.currentTimeMillis()),
            SmbFile("Música", "Música", true, 0, System.currentTimeMillis()),
            SmbFile("Fotos", "Fotos", true, 0, System.currentTimeMillis()),
            SmbFile("archivo.txt", "archivo.txt", false, 1234, System.currentTimeMillis(), "txt"),
            SmbFile("video.mp4", "video.mp4", false, 1_500_000_000, System.currentTimeMillis(), "mp4"),
        )
        fileSystem["Fotos"] = mutableListOf(
            SmbFile("vacaciones.jpg", "Fotos\\vacaciones.jpg", false, 5_000_000, System.currentTimeMillis(), "jpg"),
            SmbFile("familia.png", "Fotos\\familia.png", false, 3_200_000, System.currentTimeMillis(), "png"),
        )
    }

    override suspend fun testConnection(connection: SmbConnection): Flow<ConnectionTestResult> = flow {
        var result = ConnectionTestResult()

        result = result.copy(hostReachable = StepResult.InProgress)
        emit(result)
        delay(300)
        result = result.copy(hostReachable = StepResult.Success, portAccessible = StepResult.Success)
        emit(result)

        delay(200)
        result = result.copy(smbNegotiated = StepResult.Success)
        emit(result)

        delay(200)
        result = result.copy(authenticated = StepResult.Success)
        emit(result)

        delay(200)
        result = result.copy(shareAccessible = StepResult.Success)
        emit(result)

        delay(200)
        result = result.copy(canListFiles = StepResult.Success)
        emit(result)
    }

    override suspend fun listFiles(
        connection: SmbConnection,
        path: String
    ): Result<List<SmbFile>> {
        if (shouldFail) return Result.failure(failError)
        delay(200)
        val normalizedPath = path.replace('/', '\\').trimStart('\\')
        return Result.success(fileSystem[normalizedPath] ?: emptyList())
    }

    override suspend fun createFolder(
        connection: SmbConnection,
        path: String,
        name: String
    ): Result<Unit> {
        if (shouldFail) return Result.failure(failError)
        val parent = path.replace('/', '\\').trimStart('\\')
        val fullPath = if (parent.isEmpty()) name else "$parent\\$name"
        val list = fileSystem.getOrPut(parent) { mutableListOf() }
        list.add(SmbFile(name, fullPath, true, 0, System.currentTimeMillis()))
        fileSystem[fullPath] = mutableListOf()
        return Result.success(Unit)
    }

    override suspend fun delete(
        connection: SmbConnection,
        path: String,
        isDirectory: Boolean
    ): Result<Unit> {
        if (shouldFail) return Result.failure(failError)
        val normalized = path.replace('/', '\\').trimStart('\\')
        val parent = normalized.substringBeforeLast('\\', "")
        fileSystem[parent]?.removeAll { it.path == normalized }
        if (isDirectory) fileSystem.remove(normalized)
        return Result.success(Unit)
    }

    override suspend fun rename(
        connection: SmbConnection,
        path: String,
        newName: String
    ): Result<Unit> {
        if (shouldFail) return Result.failure(failError)
        return Result.success(Unit)
    }

    override suspend fun downloadFile(
        connection: SmbConnection,
        remotePath: String,
        outputStream: OutputStream,
        onProgress: ((Long, Long) -> Unit)?
    ): Result<Unit> {
        if (shouldFail) return Result.failure(failError)
        val data = "Fake file content".toByteArray()
        outputStream.write(data)
        onProgress?.invoke(data.size.toLong(), data.size.toLong())
        return Result.success(Unit)
    }

    override suspend fun uploadFile(
        connection: SmbConnection,
        remotePath: String,
        inputStream: InputStream,
        totalSize: Long,
        onProgress: ((Long, Long) -> Unit)?
    ): Result<Unit> {
        if (shouldFail) return Result.failure(failError)
        onProgress?.invoke(totalSize, totalSize)
        return Result.success(Unit)
    }

    override fun closeAll() {
        // No-op for fake
    }
}
