package com.example.computer_network_hw_app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatHistoryViewModel @Inject constructor(private val chatHistoryModel: ChatHistoryModel) : ViewModel() {
    private val _chatHistory = MutableStateFlow<List<ChatHistory>>(emptyList())
    val chatHistory: StateFlow<List<ChatHistory>> = _chatHistory

    init {
        viewModelScope.launch {
            while(true) {
                _chatHistory.value = try {
                    chatHistoryModel.getChatHistory()
                } catch (e: Exception) {
                    emptyList() // Handle error, e.g., show a message to the user
                }
                delay(2000) // Fetch every second
            }
        }
    }
}