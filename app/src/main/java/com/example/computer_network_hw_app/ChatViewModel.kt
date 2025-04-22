package com.example.computer_network_hw_app

import androidx.compose.foundation.ScrollState
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
    BOT,
    RECEIVING
}

@Serializable
data class ChatMessage(val message: String, val sender : Sender)

@Serializable
data class ChatResponseType(val type: String)

@Serializable
data class ChatFlow(val messageChunk : String, val isEnd : Boolean);

@Serializable
data class CreateChatResponse(val id : Int);

@Serializable
data class ChatRequest(val message: String, val id : Int);

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ChatService private val service : Service,
) : ViewModel() {
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val _serverConnected = MutableStateFlow(false)
    val serverConnected: StateFlow<Boolean> = _serverConnected

    private var id : Int? = null

    private val isReceiving = MutableStateFlow(false)
    val receiving: StateFlow<Boolean> = isReceiving

    private val _scrollState = MutableStateFlow(ScrollState(0))
    val scrollState: StateFlow<ScrollState> = _scrollState


    init {
        viewModelScope.launch {
            while(true) {
                _serverConnected.value = service.isConnected
                delay(100)
            }
        }
    }

    fun chat(message: String) {
        val newChatMessages = _chatMessages.value.toMutableList()
        newChatMessages.add(ChatMessage(message, Sender.USER))
        _chatMessages.value = newChatMessages
        scrollToMax()
        if (id == null) {
            viewModelScope.launch {
                val response = service.apiCall<CreateChatResponse, String>("createChat", "NULL")
                id = response.id
                chatRequest(message)
            }
        }
        else {
            chatRequest(message)
        }
    }

    fun changeChatId(newId: Int) {
        if(isReceiving.value) {
            return;
        }
        id = newId
        _chatMessages.value = emptyList()
        viewModelScope.launch {
            val request = mapOf(
                "id" to id!!,
            )
            val response = service.apiCall<List<ChatMessage>, Map<String, Int>>("getHistory", request)
            _chatMessages.value = response
            scrollToMax()
        }
    }

    fun startNewChat() {
        if(isReceiving.value) {
            return;
        }
        id = null
        _chatMessages.value = emptyList()
    }


    private fun scrollToMax() {
        viewModelScope.launch {
            for (i in 0..20) {
                _scrollState.value.scrollTo(_scrollState.value.maxValue)
                delay(1)
            }
        }
    }

    private fun followBottom() {
        viewModelScope.launch {
            if(_scrollState.value.maxValue - _scrollState.value.value < 400) {
                for (i in 0..20) {
                    _scrollState.value.scrollTo(_scrollState.value.maxValue)
                    delay(1)
                }
            }
        }
    }

    private fun chatRequest(message: String) {
        val body = ChatRequest(message, id!!)
        viewModelScope.launch {
            isReceiving.value = true

            val response = service.apiCall<ChatResponseType, ChatRequest>("chat", body)
            if (response.type == "Chunked") {
                var ringIndex = 0;
                val ringIcons = listOf("◴", "◵", "◶", "◷")
                while(!service.readyToRead()) {
                    val newChatMessages = _chatMessages.value.toMutableList()
                    newChatMessages.add(ChatMessage(ringIcons[ringIndex], Sender.RECEIVING))
                    ringIndex++;
                    if (ringIndex >= ringIcons.size) {
                        ringIndex = 0;
                    }
                    _chatMessages.value = newChatMessages
                    scrollToMax()
                    delay(150)
                    newChatMessages.removeAt(newChatMessages.lastIndex)
                }
                val newChatMessagesInit = _chatMessages.value.toMutableList()
                newChatMessagesInit.add(ChatMessage("", Sender.BOT))
                _chatMessages.value = newChatMessagesInit
                while (true) {
                    val chunk = service.read<ChatFlow>();
                    if (chunk.isEnd) {
                        break;
                    }
                    val newChatMessages2 = _chatMessages.value.toMutableList()
                    val botResp = ChatMessage(
                        newChatMessages2.last().message + chunk.messageChunk,
                        Sender.BOT
                    )
                    newChatMessages2.removeAt(newChatMessages2.lastIndex)
                    newChatMessages2.add(botResp)
                    _chatMessages.value = newChatMessages2
                    followBottom()
                }
            }
            isReceiving.value = false
        }
    }
}