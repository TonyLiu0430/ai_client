package com.example.computer_network_hw_app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Service @Inject constructor(private val settingsModel: SettingsModel) : CoroutineScope by MainScope() {

    private var socket : Socket? = null
    private var pollingStarted = false

    val isConnected: Boolean
        get() {
            if(socket == null) {
                return false
            }
            return socket!!.isConnected && !socket!!.isClosed
        }

    private fun startPolling() {
        if (!pollingStarted) {
            pollingStarted = true
            launch {
                while (true) {
                    if (!isConnected) {
                        println("Not connected to server, trying to reconnect...")
                        checkConnected()
                    }
                    delay(1000)
                }
            }
        }
    }

    init{
        startPolling()
    }

    public suspend fun checkConnected() : Boolean {
        if (!isConnected) {
            val serverip = settingsModel.getSetting("SERVER_IP")
            if (serverip != null) {
                val (ip, port) = serverip.split(":")
                if (ip.isEmpty() || port.isEmpty()) {
                    return false
                }
                try {
                    println("Connecting to server: $ip:$port")
                    socket = withContext(Dispatchers.IO) {
                        val socketAddress = InetSocketAddress(ip, port.toInt())
                        Socket().apply {
                            connect(socketAddress, 300)
                        }
                    }
                    println("Connected to server: $ip:$port")
                    connect(ip, port.toInt())
                } catch (e: Exception) {
                    println("Failed to connect to server: $e")
                    socket = null
                }
            }
        }
        return isConnected
    }

    private fun connect(ip : String, port : Int) {
//        socket!!.soTimeout = 1000
//        socket!!.keepAlive = true
//        socket!!.sendBufferSize = 1024 * 1024
//        socket!!.receiveBufferSize = 1024 * 1024
//        socket!!.tcpNoDelay = true
//        socket!!.reuseAddress = true
    }

    suspend inline fun<reified U, reified T> apiCall(method : String, content : T) : U {
        val resp = apiCallJson(method, Json.encodeToJsonElement(content));
        return Json.decodeFromString<U>(resp);
    }


    suspend fun apiCallJson(method : String, content : JsonElement = Json.decodeFromString("{}")) : String  {
        if (!isConnected) {
            throw Exception("Not connected to server")
        }
        val request = mapOf(
            "method" to Json.decodeFromString(method),
            "content" to content
        )
        println("Request Json : ${Json.encodeToString(request)}")
        val response = withContext(Dispatchers.IO) {
            val writer = socket!!.getOutputStream().bufferedWriter()
            writer.write(Json.encodeToString(request))
            writer.flush()
            val reader = socket!!.getInputStream().bufferedReader()
            reader.readLine()
        }
        return response
    }

    suspend inline fun<reified T> read() : T {
        val response = readPrivate();
        return Json.decodeFromString<T>(response);
    }

    suspend fun readPrivate() : String {
        if (!isConnected) {
            throw Exception("Not connected to server")
        }
        val response = withContext(Dispatchers.IO) {
            val reader = socket!!.getInputStream().bufferedReader()
            reader.readLine()
        }
        return response
    }

}