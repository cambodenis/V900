package com.example.v900.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.v900.data.AppContainer
import com.example.v900.data.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


/**
 * AppViewModel — безопасно ждёт появления репозитория в AppContainer,
 * затем подписывается на repo.devices и поддерживает _devices.
 */
class AppViewModel : ViewModel() {
    private val _devices = MutableStateFlow<List<DeviceUiModel>>(emptyList())
    val devices: StateFlow<List<DeviceUiModel>> = _devices

    init {
        viewModelScope.launch {
            // ждём появления репозитория; collectLatest автоматически отменяет предыдущую подписку
            AppContainer.repoFlow.collectLatest { repo: DeviceRepository? ->
                if (repo == null) {
                    _devices.value = emptyList()
                } else {
                    // Подписываемся на Flow<Map<String, DeviceState>>
                    repo.devices.collect { map: Map<String, com.example.v900.data.DeviceState> ->
                        // Явно преобразуем DeviceState -> DeviceUiModel, чтобы избежать ошибок вывода типов
                        val list: List<DeviceUiModel> = map.values.map { ds ->
                            DeviceUiModel(
                                id = ds.deviceId,
                                tacho = ds.tacho,
                                speed = ds.speed,
                                fuel = ds.fuel,
                                fresh_water = ds.fresh_water,
                                black_water = ds.black_water,
                                relays = ds.relays,
                                lastSeenMillis = ds.lastSeenMillis
                            )
                        }.sortedBy { ui -> ui.id }

                        _devices.value = list
                    }
                }
            }
        }
    }


}
