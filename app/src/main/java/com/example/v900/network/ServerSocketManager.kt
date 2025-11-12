package com.example.v900.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap


/**
 * ServerSocketManager — менеджер TCP-соединений.
 * Пакет: com.example.v900.network
 *
 * Ключевые особенности:
 * - length-prefixed (4 байта big-endian) JSON сообщения
 * - auth как первое сообщение
 * - heartbeat мониторинг
 * - callbacks для обработки telemetry и state
 */

class ServerSocketManager(
    private val port: Int = 12345,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val authValidator: suspend (deviceId: String, token: String?) -> Boolean,
    private val onTelemetry: suspend (deviceId: String, payload: JsonObject) -> Unit,
    private val onState: suspend (deviceId: String, payload: JsonObject) -> Unit,
    private val onClientConnected: suspend (deviceId: String) -> Unit = {},
    private val onClientDisconnected: suspend (deviceId: String) -> Unit = {}
) {
    private val TAG = "ServerSocketManager"
    private val gson = Gson()
    private var serverSocket: ServerSocket? = null

    private val connections = ConcurrentHashMap<String, ClientConnection>()
    private val connMutex = Mutex()
    private val HEARTBEAT_TIMEOUT_MS = 90_000L
    private val MAX_MESSAGE_SIZE = 64 * 1024

    data class ClientConnection(
        val socket: Socket,
        val input: DataInputStream,
        val output: DataOutputStream,
        val deviceId: String,
        val job: Job,
        @Volatile var lastSeen: Long = System.currentTimeMillis()
    )



    fun start() {
        scope.launch(Dispatchers.IO) {
            try {
                val bindAddr = InetAddress.getByName("0.0.0.0")
                serverSocket = ServerSocket(port, 50, bindAddr)
                Log.i(TAG, "Server listening on ${bindAddr.hostAddress}:$port")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind server socket: ${e.message}", e)
                stop()
                return@launch
            }

            val gson = Gson()

            while (isActive) {
                try {
                    val client = serverSocket!!.accept()
                    val remote = "${client.inetAddress.hostAddress}:${client.port}"
                    Log.i(TAG, "Accepted connection from $remote")

                    // обработка клиента в отдельной корутине
                    scope.launch(Dispatchers.IO) {
                        try {
                            val reader = BufferedReader(
                                InputStreamReader(
                                    client.getInputStream(),
                                    Charsets.UTF_8
                                )
                            )
                            val firstLine = reader.readLine() // ESP32 обычно отправляет JSON и \n
                            Log.i(TAG, "Raw input from $remote -> $firstLine")

                            if (!firstLine.isNullOrBlank()) {
                                try {
                                    val json = gson.fromJson(firstLine, JsonObject::class.java)
                                    val type = json.get("type")?.asString
                                    val deviceId = json.get("deviceId")?.asString ?: "unknown"

                                    Log.i(TAG, "Parsed JSON type=$type deviceId=$deviceId")

                                    when (type) {
                                        "telemetry" -> onTelemetry?.let { cb ->
                                            withContext(Dispatchers.Default) {
                                                cb(deviceId, json)
                                            }
                                        }
                                        "state" -> onState?.let { cb ->
                                            withContext(Dispatchers.Default) {
                                                cb(deviceId, json)
                                            }
                                        }
                                        else -> Log.w(TAG, "Unknown message type='$type' from $remote")
                                    }
                                } catch (pe: Exception) {
                                    Log.e(TAG, "JSON parse error from $remote: ${pe.message}", pe)
                                }
                            } else {
                                Log.w(TAG, "Empty input from $remote")
                            }

                            // закрыть соединение
                            client.close()
                            Log.i(TAG, "Connection closed $remote")
                        } catch (e: Exception) {
                            Log.e(TAG, "Client handling error for $remote: ${e.message}", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Accept failed: ${e.message}", e)
                }
            }
        }
    }

    private fun handleNewClient(socket: Socket) {
        val job = scope.launch {
            val input = DataInputStream(socket.getInputStream())
            val output = DataOutputStream(socket.getOutputStream())
            try {
                val raw = readMessage(input) ?: run {
                    socket.close()
                    return@launch
                }
                val json = gson.fromJson(raw, JsonObject::class.java)
                val type = json.get("type")?.asString
                if (type != "auth") {
                    Log.w(TAG, "First message not auth from ${socket.inetAddress.hostAddress}")
                    socket.close()
                    return@launch
                }
                val deviceId = json.get("deviceId")?.asString ?: generateDeviceId(socket)
                val token = json.get("token")?.asString
                val ok = try { authValidator(deviceId, token) } catch (e: Exception) { false }
                if (!ok) {
                    Log.w(TAG, "Auth failed for $deviceId")
                    sendMessageInternal(output, gson.toJson(mapOf("type" to "auth_response", "status" to "denied")))
                    socket.close()
                    return@launch
                }
                val conn = ClientConnection(socket, input, output, deviceId, coroutineContext[Job]!!)
                connections[deviceId] = conn
                Log.i(TAG, "Client authenticated: $deviceId")
                onClientConnected(deviceId)
                sendMessageInternal(output, gson.toJson(mapOf("type" to "auth_response", "status" to "ok")))
                while (isActive && !socket.isClosed) {
                    val msg = readMessage(input) ?: break
                    conn.lastSeen = System.currentTimeMillis()
                    processIncoming(deviceId, msg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Client handler error: ${e.message}", e)
            } finally {
                cleanupConnection(socket)
            }
        }
    }

    private suspend fun processIncoming(deviceId: String, rawJson: String) {
        try {
            val obj = gson.fromJson(rawJson, JsonObject::class.java)
            val type = obj.get("type")?.asString ?: return
            when (type) {
                "telemetry" -> {
                    val payload = obj.getAsJsonObject("payload") ?: JsonObject()
                    onTelemetry(deviceId, payload)
                }
                "state" -> {
                    val payload = obj.getAsJsonObject("payload") ?: JsonObject()
                    onState(deviceId, payload)
                }
                "heartbeat" -> {
                    // lastSeen уже обновлён
                }
                "cmd_response" -> {
                    Log.d(TAG, "Cmd response from $deviceId: $rawJson")
                }
                else -> {
                    Log.w(TAG, "Unknown type from $deviceId: $type")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "processIncoming parse error: ${e.message}")
        }
    }

    suspend fun sendCommand(deviceId: String, commandJson: String): Boolean {
        val conn = connections[deviceId] ?: return false
        return try {
            withContext(Dispatchers.IO) {
                sendMessageInternal(conn.output, commandJson)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "sendCommand error: ${e.message}")
            false
        }
    }

    private fun sendMessageInternal(output: DataOutputStream, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        output.writeInt(bytes.size)
        output.write(bytes)
        output.flush()
    }

    private fun readMessage(input: DataInputStream): String? {
        return try {
            val len = input.readInt()
            if (len <= 0 || len > MAX_MESSAGE_SIZE) return null
            val buf = ByteArray(len)
            input.readFully(buf)
            String(buf, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun monitorHeartbeats() {
        while (scope.isActive) {
            val now = System.currentTimeMillis()
            val toRemove = mutableListOf<String>()
            for ((id, conn) in connections) {
                if (now - conn.lastSeen > HEARTBEAT_TIMEOUT_MS) {
                    Log.w(TAG, "Heartbeat timeout: $id")
                    toRemove.add(id)
                }
            }
            for (id in toRemove) {
                connections[id]?.socket?.close()
                connections.remove(id)?.let { onClientDisconnected(id) }
            }
            delay(10_000L)
        }
    }


    private fun cleanupConnection(socket: Socket) {
        val deviceId = connections.entries.find { it.value.socket == socket }?.key
        if (deviceId != null) {
            connections.remove(deviceId)
            scope.launch { // Запускаем в scope для колбэка
                onClientDisconnected(deviceId)
                Log.i(TAG, "Client disconnected and cleaned up: $deviceId")
            }
        }
        try {
            socket.close() // Закрываем сокет независимо от всего
        } catch (_: Exception) {}
    }

    fun stop() {
        scope.cancel() // Отменяем все корутины, включая цикл accept()
        // Создаем отдельную корутину для очистки, которая не отменится
        CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket?.close()
            } catch (_: Exception) {}
            connections.values.forEach {
                try {
                    it.socket.close()
                } catch (_: Exception) {}
            }
            connections.clear()
            Log.i(TAG, "Server stopped and all connections closed.")
        }
    }

    fun listConnectedDevices(): List<String> = connections.keys().toList()

    private fun generateDeviceId(socket: Socket): String = "${socket.inetAddress.hostAddress}:${socket.port}"
}
