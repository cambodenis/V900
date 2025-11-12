package com.example.v900.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.v900.data.DeviceRepository
import com.example.v900.data.PrefsManager

/**
 * Fabricа для DeviceListViewModel — предоставляет конструкцию с репозиторием и prefs.
 * Используйте в Activity/Fragment: val vm: DeviceListViewModel = viewModel(factory = DeviceListViewModelFactory(...))
 */
class DeviceListViewModelFactory(
    private val repo: DeviceRepository,
    private val prefs: PrefsManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeviceListViewModel::class.java)) {
            return DeviceListViewModel(repo, prefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
