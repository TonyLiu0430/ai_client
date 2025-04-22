package com.example.computer_network_hw_app

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
import javax.inject.Qualifier
import javax.inject.Singleton

//class TcpStreamManager(private val socket : Socket) {
//    private val streams = mutableMapOf<Int, TcpStream>()
//    private val buffers = mutableMapOf<Int, MutableList<String>>()
//    private var idCnt : Int = 0;
//
//    fun getTcpStream() : TcpStream {
//        val newStream = TcpStream(this, idCnt)
//        streams[idCnt] = newStream
//        idCnt += 2;
//        return newStream
//    }
//
//    suspend fun send(tcpStream : TcpStream, data : String) {
//        withContext(Dispatchers.IO) {
//            val writer = socket.getOutputStream().bufferedWriter()
//            writer.write("${String.format("%04X", tcpStream.id)}$data\n")
//            writer.flush()
//        }
//    }
//
//    suspend fun readLine(tcpStream : TcpStream) : String {
//        if (!buffers.containsKey(tcpStream.id)) {
//            buffers[tcpStream.id] = mutableListOf()
//        }
//        val buffer = buffers[tcpStream.id]!!
//        if (buffer.isNotEmpty()) {
//            return buffer.removeAt(0)
//        }
//        withContext(Dispatchers.IO) {
//            val reader = socket.getInputStream().bufferedReader()
//            while (true) {
//                val line = reader.readLine() ?: break
//                val id = line.substring(0, 4).toInt(16)
//                val data = line.substring(4)
//                if (id == tcpStream.id) {
//                    return@withContext data
//                } else {
//                    if (!buffers.containsKey(id)) {
//                        buffers[id] = mutableListOf()
//                    }
//                    buffers[id]!!.add(data)
//                }
//            }
//        }
//    }
//}
//
//
//class TcpStream(private val manager : TcpStreamManager, public val id : Int) {
//    suspend fun send(data : String) {
//        manager.send(this, data);
//    }
//
//    suspend fun readLine() : String {
//        return manager.readLine(this);
//    }
//}


@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    @ChatService
    fun provideChatService(settingsModel: SettingsModel): Service {
        return Service(settingsModel);
    }

    @Provides
    @Singleton
    @ChatHistoryService
    fun provideChatHistoryService(settingsModel: SettingsModel): Service {
        return Service(settingsModel);
    }
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ChatHistoryService

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ChatService

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

    private suspend fun checkConnected() : Boolean {
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
                            connect(socketAddress, 5000)
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

    private suspend fun reConnect() {
        socket = null
        checkConnected();
    }

    private fun connect(ip : String, port : Int) {
        socket!!.soTimeout = 150000
        socket!!.keepAlive = true
        socket!!.sendBufferSize = 1024 * 1024
        socket!!.receiveBufferSize = 1024 * 1024
        socket!!.tcpNoDelay = true
        socket!!.reuseAddress = true
    }

    var tryCnt : Int = 0;

    suspend inline fun<reified U, reified T> apiCall(method : String, content : T) : U {
        tryCnt = 0;
        val resp = apiCallJson(method, Json.encodeToJsonElement(content));
        return Json.decodeFromString<U>(resp);
    }


    suspend fun apiCallJson(method : String, content : JsonElement = Json.decodeFromString("{}")) : String  {
        if (tryCnt > 3) {
            socket = null
            throw Exception("Failed to connect to server")
        }
        val request = mapOf(
            "method" to Json.decodeFromString(method),
            "content" to content
        )
        try {
            val response = withContext(Dispatchers.IO) {
                val writer = socket!!.getOutputStream().bufferedWriter()
                writer.write(Json.encodeToString(request))
                writer.flush()
                val reader = socket!!.getInputStream().bufferedReader()
                reader.readLine()
            }
            return response
        } catch(e: Exception) {
            reConnect()
            tryCnt++
            return apiCallJson(method, content)
        }
    }

    fun readyToRead() : Boolean {
        return (socket?.getInputStream()?.available() ?: 0) > 0;
    }

    suspend inline fun<reified T> read() : T {
        tryCnt = 0;
        val response = readPrivate();
        return Json.decodeFromString<T>(response);
    }

    suspend fun readPrivate() : String {
        if (tryCnt > 3) {
            socket = null
            throw Exception("Failed to connect to server")
        }
        try {
            val response = withContext(Dispatchers.IO) {
                val reader = socket!!.getInputStream().bufferedReader()
                reader.readLine()
            }
            return response
        }catch(e: Exception) {
            reConnect()
            tryCnt++
            return readPrivate()
        }
    }

}