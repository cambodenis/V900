package com.example.v900.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.v900.data.DeviceRepository
import com.example.v900.data.PrefsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DeviceListViewModel(
    private val repo: DeviceRepository,
    private val prefs: PrefsManager
) : ViewModel() {

    private val _devices = MutableStateFlow<List<DeviceUiModel>>(emptyList())
    val devices: StateFlow<List<DeviceUiModel>> = _devices

    init {
        // Подписываемся на Flow репозитория — обновляем UI при каждом изменении
        viewModelScope.launch {
            repo.devices.collect { map ->
                // map: Map<String, DeviceState>
                val list = map.values.map { it.toUiModel() }.sortedBy { it.id }
                _devices.value = list
            }
        }
    }

    fun toggleRelay(deviceId: String, relay: String, value: Boolean) {
        // Сохраняем локально и/или отправляем команду на устройство через ServerSocketManager
        prefs.saveRelayState(deviceId, relay, value)
        // TODO: вызвать серверный метод sendCommand(...) для реального действия
    }
}
