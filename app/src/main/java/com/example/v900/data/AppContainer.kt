package com.example.v900.data

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

    fun setRepo(repo: DeviceRepository) {
        _repoFlow.value = repo
    }

    // Удобный доступ (могут понадобиться в некоторых местах)
    fun getRepo(): DeviceRepository? = _repoFlow.value
}
