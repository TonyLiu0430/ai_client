package com.example.computer_network_hw_app

import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton


@Serializable
data class ChatHistory(val id : Int, val title: String);

@Singleton
class ChatHistoryModel @Inject constructor(@ChatHistoryService private val service : Service) {

    suspend fun getChatHistory(): List<ChatHistory> {
        if (!service.isConnected) {
            throw Exception("Not connected to server")
        }
        val response = service.apiCall<List<ChatHistory>, String>("getChatHistories", "NULL")
        return response
    }
}