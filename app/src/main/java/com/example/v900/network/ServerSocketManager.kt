package com.example.v900.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

/**
 * ServerSocketManager — менеджер TCP-соединений.
 * Протокол (Гибридный):
 * 1. Handshake: Текстовая строка JSON + \n (читаем побайтово до \n).
 * 2. Loop: Framed (4 байта длины + JSON).
 * 3. Send: Framed (4 байта длины + JSON).
 */
class ServerSocketManager(
    private val port: Int = 12345,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val authValidator: suspend (deviceId: String, token: String?) -> Boolean,
    private val onTelemetry: suspend (deviceId: String, payload: JsonObject) -> Unit,
    private val onState: suspend (deviceId: String, payload: JsonObject) -> Unit,
    private val onClientConnected: ((deviceId: String) -> Unit)? = null,
    private val onClientDisconnected: ((deviceId: String) -> Unit)? = null
) {
    private val TAG = "ServerSocketManager"
    private val gson = Gson()
    private var serverSocket: ServerSocket? = null

    private val connections = ConcurrentHashMap<String, ClientConnection>()

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
                    handleClient(client)
                } catch (e: Exception) {
                    Log.e(TAG, "Accept failed: ${e.message}", e)
                }
            }
        }
    }

    private fun handleClient(socket: Socket) {
        scope.launch(Dispatchers.IO) {
            val remote = "${socket.inetAddress.hostAddress}:${socket.port}"
            try {
                Log.i(TAG, "Accepted connection from $remote")
                // Используем BufferedInputStream для эффективности, но НЕ BufferedReader
                val bis = BufferedInputStream(socket.getInputStream())
                val bos = BufferedOutputStream(socket.getOutputStream())
                val din = DataInputStream(bis)
                val dout = DataOutputStream(bos)

                // 1. Handshake: Читаем строку до \n вручную
                val firstLine = readLineStrict(din)

                if (firstLine.isEmpty()) {
                    Log.w(TAG, "Empty handshake from $remote")
                    socket.close()
                    return@launch
                }

                val element = try {
                    JsonParser.parseString(firstLine)
                } catch (e: Exception) {
                    Log.e(TAG, "Invalid JSON in handshake from $remote: $firstLine")
                    socket.close()
                    return@launch
                }

                if (!element.isJsonObject) {
                    Log.e(TAG, "Handshake JSON is not an object from $remote: $firstLine")
                    socket.close()
                    return@launch
                }

                val json = element.asJsonObject
                val deviceId = json.get("deviceId")?.asString ?: generateDeviceId(socket)
                val token = json.get("token")?.asString
                val type = json.get("type")?.asString

                // 2. Auth
                if (!authValidator(deviceId, token)) {
                    Log.w(TAG, "Auth failed for $deviceId from $remote")
                    // Можно отправить ответ об ошибке
                    sendJsonFramed(
                        dout,
                        gson.toJson(mapOf("type" to "auth_response", "status" to "denied"))
                    )
                    socket.close()
                    return@launch
                }

                Log.i(TAG, "Client authenticated: $deviceId ($type)")

                // Отправляем успешный ответ (Framed), так как ESP может его ждать
                sendJsonFramed(
                    dout,
                    gson.toJson(mapOf("type" to "auth_response", "status" to "ok"))
                )

                // 3. Register and Loop
                registerClient(deviceId, socket, din, dout)

                if (type == "telemetry") onTelemetry(deviceId, json)
                else if (type == "state") onState(deviceId, json)

            } catch (e: Exception) {
                Log.e(TAG, "Handshake error $remote: ${e.message}", e)
                try {
                    socket.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    /**
     * Читает байты до символа '\n'. Игнорирует '\r'.
     * Блокирует поток.
     */
    private fun readLineStrict(din: DataInputStream): String {
        val baos = ByteArrayOutputStream()
        while (true) {
            val b = try {
                din.readByte()
            } catch (e: Exception) {
                // EOF or error
                break
            }
            if (b == '\n'.code.toByte()) {
                break
            }
            if (b != '\r'.code.toByte()) {
                baos.write(b.toInt())
            }
        }
        return baos.toString("UTF-8")
    }

    // Вспомогательный метод для отправки framed сообщения напрямую в поток
    private fun sendJsonFramed(output: DataOutputStream, json: String) {
        try {
            val bytes = json.toByteArray(Charsets.UTF_8)
            synchronized(output) {
                output.writeInt(bytes.size)
                output.write(bytes)
                output.flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending framed response", e)
        }
    }

    fun stop() {
        scope.cancel()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket?.close()
            } catch (_: Exception) {
            }
            connections.values.forEach {
                try {
                    it.socket.close()
                } catch (_: Exception) {
                }
            }
            connections.clear()
        }
    }

    private fun generateDeviceId(socket: Socket): String = "${socket.inetAddress.hostAddress}:${socket.port}"

    private fun registerClient(deviceId: String, socket: Socket, din: DataInputStream, dout: DataOutputStream) {
        connections[deviceId]?.let {
            try { it.socket.close() } catch (_: Exception) {}
            try {
                it.job.cancel()
            } catch (_: Exception) {
            }
            connections.remove(deviceId)
        }

        val job = scope.launch(Dispatchers.IO) {
            try {
                while (isActive && !socket.isClosed) {
                    // Loop: Framed Messages (4 bytes length + JSON)
                    val length = try {
                        din.readInt()
                    } catch (e: Exception) {
                        break
                    }

                    if (length > 1_000_000 || length < 0) {
                        Log.w(TAG, "Invalid message length $length from $deviceId")
                        break
                    }

                    val payloadBytes = ByteArray(length)
                    din.readFully(payloadBytes)
                    val msg = String(payloadBytes, Charsets.UTF_8)

                    connections[deviceId]?.lastSeen = System.currentTimeMillis()

                    try {
                        val element = JsonParser.parseString(msg)
                        if (element.isJsonObject) {
                            val json = element.asJsonObject
                            when (json.get("type")?.asString) {
                                "telemetry" -> onTelemetry(deviceId, json)
                                "state" -> onState(deviceId, json)
                                "ack" -> Log.d(TAG, "ACK $deviceId")
                                else -> Log.d(TAG, "Unknown type from $deviceId: $msg")
                            }
                        } else {
                            Log.w(TAG, "Received primitive/array from $deviceId: $msg")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Parse error from $deviceId. Raw: '$msg'", e)
                    }
                }
            } catch (e: Exception) {
                // socket closed or error
            } finally {
                unregisterClient(deviceId)
            }
        }

        val conn = ClientConnection(socket, din, dout, deviceId, job)
        connections[deviceId] = conn
        onClientConnected?.invoke(deviceId)
    }

    private fun unregisterClient(deviceId: String) {
        connections.remove(deviceId)?.let {
            try {
                it.socket.close()
            } catch (_: Exception) {
            }
            onClientDisconnected?.invoke(deviceId)
        }
    }

    suspend fun sendCommand(deviceId: String, commandJson: String): Boolean {
        val conn = connections[deviceId] ?: return false
        return try {
            withContext(Dispatchers.IO) {
                val bytes = commandJson.toByteArray(Charsets.UTF_8)
                synchronized(conn.output) {
                    conn.output.writeInt(bytes.size) // 4 bytes length
                    conn.output.write(bytes)         // payload
                    conn.output.flush()
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "sendCommand failed for $deviceId", e)
            false
        }
    }
}
