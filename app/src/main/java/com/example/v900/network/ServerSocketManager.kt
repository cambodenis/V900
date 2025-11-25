package com.example.v900.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.BufferedReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

/**
 * ServerSocketManager — менеджер TCP-соединений.
 * Пакет: com.example.v900.network
 *
 * Особенности:
 * - при подключении читаем первое сообщение (handshake / telemetry) через readLine()
 * - затем регистрируем клиента и держим socket открытым
 * - поддерживаем map connections: deviceId -> ClientConnection
 * - отправка команд реализована через length-prefixed 4-byte BE + payload (UTF-8)
 * - per-client reader loop обновляет lastSeen и обрабатывает входящие сообщения
 */
class ServerSocketManager(
    private val port: Int = 12345,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val onTelemetry: suspend (deviceId: String, payload: JsonObject) -> Unit,
    private val onState: suspend (deviceId: String, payload: JsonObject) -> Unit,
    private val onClientConnected: ((deviceId: String) -> Unit)? = null,
    private val onClientDisconnected: ((deviceId: String) -> Unit)? = null
) {
    private val TAG = "ServerSocketManager"
    private val gson = Gson()
    private var serverSocket: ServerSocket? = null

    // deviceId -> ClientConnection
    private val connections = ConcurrentHashMap<String, ClientConnection>()
    private val connMutex = Mutex()

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

            while (isActive) {
                try {
                    val client = serverSocket!!.accept()
                    val remote = "${client.inetAddress.hostAddress}:${client.port}"
                    Log.i(TAG, "Accepted connection from $remote")

                    // handle initial handshake in short coroutine
                    scope.launch(Dispatchers.IO) {
                        try {
                            // Wrap streams with buffered streams for performance
                            val bis = BufferedInputStream(client.getInputStream())
                            val bos = BufferedOutputStream(client.getOutputStream())
                            val din = DataInputStream(bis)
                            val dout = DataOutputStream(bos)

                            // Read first-line JSON (ESP often sends JSON + '\n')
                            val reader = BufferedReader(InputStreamReader(bis, Charsets.UTF_8))
                            val firstLine = reader.readLine()
                            Log.i(TAG, "Raw input from $remote -> $firstLine")

                            if (!firstLine.isNullOrBlank()) {
                                try {
                                    val json = gson.fromJson(firstLine, JsonObject::class.java)
                                    val type = json.get("type")?.asString
                                    val deviceId = json.get("deviceId")?.asString ?: generateDeviceId(client)

                                    Log.i(TAG, "Parsed JSON type=$type deviceId=$deviceId")

                                    // register client and start persistent reader loop
                                    registerClient(deviceId, client, din, dout)

                                    // call appropriate callback for first message
                                    when (type) {
                                        "telemetry" -> {
                                            withContext(Dispatchers.Default) { onTelemetry(deviceId, json) }
                                        }
                                        "state" -> {
                                            withContext(Dispatchers.Default) { onState(deviceId, json) }
                                        }
                                    }
                                } catch (pe: Exception) {
                                    Log.e(TAG, "JSON parse error from $remote: ${pe.message}", pe)
                                    try { client.close() } catch (_: Exception) {}
                                }
                            } else {
                                Log.w(TAG, "Empty initial input from $remote")
                                try { client.close() } catch (_: Exception) {}
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Initial handshake error for $remote: ${e.message}", e)
                            try { client.close() } catch (_: Exception) {}
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Accept failed: ${e.message}", e)
                }
            }
        }
    }

    fun stop() {
        scope.cancel()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket?.close()
            } catch (_: Exception) {}
            connections.values.forEach {
                try { it.socket.close() } catch (_: Exception) {}
            }
            connections.clear()
            Log.i(TAG, "Server stopped and all connections closed.")
        }
    }

    private fun generateDeviceId(socket: Socket): String = "${socket.inetAddress.hostAddress}:${socket.port}"

    private fun registerClient(deviceId: String, socket: Socket, din: DataInputStream, dout: DataOutputStream) {
        // if exists, close previous
        connections[deviceId]?.let {
            try { it.socket.close() } catch (_: Exception) {}
            connections.remove(deviceId)
        }

        // launch reader loop for this client
        val job = scope.launch(Dispatchers.IO) {
            try {
                val conn = connections[deviceId] // may be null until we insert below
                // keep reading length-prefixed messages (4 byte big-endian length then payload)
                while (isActive && !socket.isClosed) {
                    // read 4 bytes length
                    val lenBytes = ByteArray(4)
                    din.readFully(lenBytes) // will block until 4 bytes or throw EOF
                    val length = ByteBuffer.wrap(lenBytes).int
                    if (length <= 0 || length > 10_000_000) {
                        Log.w(TAG, "Invalid incoming length=$length from $deviceId; closing")
                        break
                    }
                    val payload = ByteArray(length)
                    din.readFully(payload)
                    val s = String(payload, Charsets.UTF_8)
                    // update lastSeen
                    connections[deviceId]?.lastSeen = System.currentTimeMillis()
                    // try parse
                    try {
                        val json = gson.fromJson(s, JsonObject::class.java)
                        val type = json.get("type")?.asString
                        when (type) {
                            "telemetry" -> withContext(Dispatchers.Default) { onTelemetry(deviceId, json) }
                            "state" -> withContext(Dispatchers.Default) { onState(deviceId, json) }
                            "ack" -> Log.i(TAG, "ACK from $deviceId -> $json")
                            else -> Log.w(TAG, "Unknown message type '$type' from $deviceId")
                        }
                    } catch (pe: Exception) {
                        Log.e(TAG, "Failed parse payload from $deviceId: ${pe.message}", pe)
                    }
                }
            } catch (e: Exception) {
                Log.i(TAG, "Reader loop ended for $deviceId: ${e.message}")
            } finally {
                // cleanup on disconnect
                unregisterClient(deviceId)
                onClientDisconnected?.invoke(deviceId)
            }
        }

        val conn = ClientConnection(socket = socket, input = din, output = dout, deviceId = deviceId, job = job)
        connections[deviceId] = conn
        onClientConnected?.invoke(deviceId)
        Log.i(TAG, "Client registered: $deviceId (total=${connections.size})")
    }

    private fun unregisterClient(deviceId: String) {
        connections.remove(deviceId)?.let { conn ->
            try { conn.socket.close() } catch (_: Exception) {}
            try { conn.job.cancel() } catch (_: Exception) {}
            Log.i(TAG, "Client unregistered: $deviceId")
        }
    }

    /**
     * Send JSON to connected device (length-prefixed: 4 byte big-endian + UTF-8 payload + flush).
     * Returns true if send succeeded.
     */
    suspend fun sendToDevice(deviceId: String, obj: JsonObject): Boolean {
        val conn = connections[deviceId] ?: return false
        return try {
            withContext(Dispatchers.IO) {
                val payload = gson.toJson(obj).toByteArray(Charsets.UTF_8)
                val lenBuf = ByteBuffer.allocate(4).putInt(payload.size).array()
                conn.output.write(lenBuf)
                conn.output.write(payload)
                conn.output.flush()
                conn.lastSeen = System.currentTimeMillis()
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendToDevice failed for $deviceId: ${e.message}", e)
            // cleanup after failure
            unregisterClient(deviceId)
            false
        }
    }
}
