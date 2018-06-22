package cf.dogo.server.bridge

import org.json.JSONObject
import java.io.PrintStream
import java.net.Socket
import java.util.*
import kotlin.collections.HashMap

class Client constructor(ip : String, port : Int, name : String){
    private val requests = HashMap<Int, Request>()
    val name = name

    val ip = ip
    val port = port
    var socket : Socket? = null
    var input : Scanner? = null
    var output : PrintStream? = null
    val listener = InputListener(this)

    init {
        connect()
    }

    fun connect(){
        for (i in 1..3){
            println("Trying to connect to server...")
            try{
                this.socket = Socket(ip, port)
                this.input = Scanner(this.socket?.getInputStream())
                this.output = PrintStream(socket?.getOutputStream())

                listener.start()
                println("Connected!")
                break
            } catch (ex : Exception) {
                println("Connection Failed!")
            }
        }
    }

    fun isAvailabe() = socket != null && socket?.isConnected as Boolean && !(socket?.isClosed as Boolean)

    fun request(request : JSONObject) : JSONObject {
        return if(isAvailabe()){
            val req = Request()
            requests[req.id] = req
            output?.println(JSONObject().put("id", req.id).put("data", request))
            val start = System.currentTimeMillis()
            while(req.response == null){
                if((System.currentTimeMillis() - start) <= 12000) {
                    Thread.sleep(100)
                } else {
                    requests.remove(req.id)
                    return JSONObject().put("desc", "time out").put("status", 408)
                }
            }
            req.response as JSONObject
        } else {
            JSONObject().put("desc", "socket is disconnected").put("status", 503)
        }
    }

    class InputListener(val con : Client) : Thread("${con.name} InputListener"){
        override fun run() {
            while (true) {
                if(con.isAvailabe() && con.input?.hasNextLine() as Boolean){
                    val json = JSONObject(con.input?.nextLine())
                    if(json.has("id")) {
                        val req = con.requests[json.getInt("id")]
                        if (req != null) {
                            req.response = json.getJSONObject("data")
                        }
                    }
                }
            }
        }
    }

}