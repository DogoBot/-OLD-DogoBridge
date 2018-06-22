package cf.dogo.server.bridge

import org.json.JSONObject
import java.util.*

class Request {
    val id = Random().nextInt()
    var response : JSONObject? = null
}