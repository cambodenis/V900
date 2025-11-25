package com.example.v900.data

import com.example.v900.network.ServerSocketManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Централизованный провайдер репозитория.
 * ForegroundCommService устанавливает repo через setRepo().
 * UI/ViewModel подписываются на repoFlow и начинают работу, когда repo != null.
 */
object AppContainer {
    private val _repoFlow = MutableStateFlow<DeviceRepository?>(null)
    val repoFlow: StateFlow<DeviceRepository?> = _repoFlow

    // количество подключённых клиентов (в реальном времени)
    private val _connectedClients = kotlinx.coroutines.flow.MutableStateFlow(0)
    val connectedClients: kotlinx.coroutines.flow.StateFlow<Int> = _connectedClients

    fun setConnectedClients(count: Int) {
        _connectedClients.value = count
    }
    // внутри object AppContainer
    private var _serverManager: ServerSocketManager? = null
    fun setServerManager(mgr: ServerSocketManager?) {
        _serverManager = mgr
    }
    // Флаг состояния сервера (true = сервер запущен)
    private val _serverRunning = kotlinx.coroutines.flow.MutableStateFlow(false)
    val serverRunning: kotlinx.coroutines.flow.StateFlow<Boolean> = _serverRunning

    fun setServerRunning(isRunning: Boolean) {
        _serverRunning.value = isRunning
    }

    fun getServerManager(): ServerSocketManager? = _serverManager
    fun setRepo(repo: DeviceRepository) {
        _repoFlow.value = repo
    }

    // Удобный доступ (могут понадобиться в некоторых местах)
    fun getRepo(): DeviceRepository? = _repoFlow.value
}
