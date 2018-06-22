package cf.dogo.server.bridge

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import kotlin.collections.ArrayList

abstract class Server constructor(val port : Int = 4676, val name : String, var logger : PrintStream = System.out) {
    var socket = ServerSocket(port)
    var connections = ArrayList<Socket>()
    val connectionListener = ConnectionListener(this)
    val inputListener = InputListener(this)





    class ConnectionListener(val srv : Server) : Thread("${srv.name} ConnectionListener") {
        override fun run() {
            super.run()
            while(true) {
                var s = srv.socket.accept()
                if(!srv.connections.contains(s)){
                    srv.logger.println("[${this.name}] Connection Received! From ${s.remoteSocketAddress}")
                    srv.connections.add(s)
                }
                for(s2 in srv.connections){
                    if(s2.isClosed || !s2.isConnected) srv.connections.remove(s2)
                }
            }
        }
    }
    class InputListener(val srv : Server) : Thread("${srv.name} InputListener") {
        override fun run() {
            super.run()
            while(true){
                for(sck in srv.connections){
                    try {
                        if (!sck.isClosed && sck.isConnected) {
                            val scan = BufferedReader(InputStreamReader(sck.getInputStream()))
                            val content = scan.readLine()
                            if (content != null) {
                                val json = JSONObject(content)
                                val response = JSONObject()
                                        .put(
                                                "data",
                                                srv.onRequest(json.getInt("id"), json.getJSONObject("data"), sck)
                                        )
                                        .put("id", json.getInt("id"))
                                PrintStream(sck.getOutputStream()).println(response.toString())
                            }
                        }
                    }catch (ex : SocketException){
                        srv.connections.remove(sck)
                    }
                }
            }
        }
    }

    abstract fun onRequest(reqid : Int, data : JSONObject, sck : Socket) : JSONObject

    fun start(){
        connectionListener.start()
        inputListener.start()
    }

    fun stop(){
        connectionListener.stop()
        inputListener.stop()
        connections.forEach { c -> c.close() }
        connections.clear()
    }
}