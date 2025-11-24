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
    private val onTelemetry: suspend (deviceId: String, payload: JsonObject) -> Unit,
    private val onState: suspend (deviceId: String, payload: JsonObject) -> Unit,

) {
    private val TAG = "ServerSocketManager"
    private val gson = Gson()
    private var serverSocket: ServerSocket? = null

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
