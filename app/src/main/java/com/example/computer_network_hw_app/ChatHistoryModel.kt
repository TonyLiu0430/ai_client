package com.example.computer_network_hw_app

import javax.inject.Inject
import javax.inject.Singleton

class ChatHistory(public val id: Int, public val title: String) {
}

@Singleton
class ChatHistoryModel @Inject constructor(private val service : Service) {

    suspend fun getChatHistory(): List<ChatHistory> {
        if (!service.isConnected) {
            throw Exception("Not connected to server")
        }
        val response = {};//service.apiCall("getChatHistory", {})
        val chatHistories = mutableListOf<ChatHistory>()
        if (response is List<*>) {
            for (item in response) {
                if (item is Map<*, *>) {
                    val chatHistory = ChatHistory(item["id"] as Int, item["title"] as String)
                    chatHistories.add(chatHistory)
                }
            }
        } else {
            throw Exception("Invalid response from server")
        }
        return chatHistories
    }
}