/*
* Copyright 2015 Kynetics SRL
*
* This file is part of orobot.
*
* orobot is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* orobot is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Affero General Public License for more details.
*
* You should have received a copy of the GNU Affero General Public License
* along with orobot.  If not, see <http://www.gnu.org/licenses/>.
*/
package websockets

import java.net.URI
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_17
import org.java_websocket.handshake.ServerHandshake
import collection.JavaConversions._
import scala.collection.mutable.ListBuffer

class ClientInteraction(port: Int, headers: Map[String, String]) {

  val messages = ListBuffer[String]()

  val client = new WebSocketClient(URI.create(s"ws://localhost:$port/wsconnect"),
    new Draft_17(), headers, 0) {

    def onError(p1: Exception) {
      println(s"WebSocket Client - onError ${p1.getMessage}")
    }

    def onMessage(message: String) {
      messages += message
      println(s"WebSocket Client - onMessage, message = $message")
    }

    def onClose(code: Int, reason: String, remote: Boolean) {
      println(s"WebSocket Client - onClose code=$code reason=$reason remote=$remote")
    }

    def onOpen(handshakedata: ServerHandshake) {
      println("WebSocket Client - onOpen")
    }
  }

}
