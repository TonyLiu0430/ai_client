package com.example.computer_network_hw_app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

enum class Sender {
    USER,
    BOT
}

@Serializable
data class ChatMessage(val message: String, val sender : Sender)

@Serializable
data class ChatResponseType(val type: String)

@Serializable
data class ChatFlow(val messageChunk : String, val isEnd : Boolean);

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val service : Service,
) : ViewModel() {
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val _serverConnected = MutableStateFlow(false)
    val serverConnected: StateFlow<Boolean> = _serverConnected

    init {
        viewModelScope.launch {
            while(true) {
                _serverConnected.value = service.isConnected
                delay(100)
            }

        }
    }

    fun chat(message: String) {
        if (!service.isConnected) {
            throw Exception("Not connected to server")
        }
        val body = ChatMessage(message, Sender.USER)
        val newChatMessages = _chatMessages.value.toMutableList()
        newChatMessages.add(body)
        _chatMessages.value = newChatMessages
        viewModelScope.launch {
            val response = service.apiCall<ChatResponseType, ChatMessage>("chat", body)
            val newChatMessagesInit = _chatMessages.value.toMutableList()
            newChatMessagesInit.add(ChatMessage("", Sender.BOT))
            _chatMessages.value = newChatMessagesInit
            if(response.type == "Chunked") {
                while(true) {
                    val chunk = service.read<ChatFlow>();
                    if(chunk.isEnd) {
                        break;
                    }
                    val newChatMessages2 = _chatMessages.value.toMutableList()
                    val botResp = ChatMessage(newChatMessages2.last().message + chunk.messageChunk, Sender.BOT)
                    newChatMessages2.removeAt(newChatMessages2.lastIndex)
                    newChatMessages2.add(botResp)
                    _chatMessages.value = newChatMessages2
                }
            }
//            val newChatMessages2 = _chatMessages.value.toMutableList()
//            newChatMessages2.add(response)
//            _chatMessages.value = newChatMessages2
        }
//        val response = viewModelScope.launch {
//            service.apiCall(message, body) as ChatMessage
//        }
//
//        val newChatMessages = _chatMessages.value.toMutableList()
//        newChatMessages.add(body)
//        newChatMessages.add(response)1
    }
}