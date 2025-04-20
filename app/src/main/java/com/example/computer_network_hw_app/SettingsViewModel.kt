package com.example.computer_network_hw_app

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val settingsModel: SettingsModel) : ViewModel() {
    fun getSetting(key: String): String? {
        return settingsModel.getSetting(key)
    }

    fun setSetting(key: String, value: String) {
        settingsModel.setSetting(key, value)
    }
}