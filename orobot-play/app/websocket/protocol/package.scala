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
package websocket

import java.util.UUID

import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.WebSocket.FrameFormatter

package object protocol {

  trait WSPayload {
    def toJson: Option[JsValue] = None
    def header: String
  }

  case object WrongMessage extends WSPayload {
    override val header = "WRONG_MSG"
  }

  case class WSMessage(header: String, payload: Option[JsValue])

  object WSMessage {
    implicit val wsMessageFormat = Json.format[WSMessage]
    implicit val wsMessageFrameFormatter = FrameFormatter.jsonFrame[WSMessage]
  }

  object StartWatching {
    implicit val watchedByFormat = Json.format[StartWatching]
    val header = "START_WATCHING"
  }

  case class StartWatching(robotId: UUID) extends WSPayload {
    override def toJson: Option[JsValue] = Some(Json.toJson(this))
    override val header = StartWatching.header
  }

  object Position {
    implicit val positionFormat = Json.format[Position]
    val header = "POSITION"
  }

  case class Position(x: Double, y: Double, h: Double) extends WSPayload {
    override def toJson: Option[JsValue] = Some(Json.toJson(this))
    override val header = Position.header
  }

  object MoveTo {
    implicit val moveToFormat = Json.format[MoveTo]
    val header = "MOVETO"
  }

  case class MoveTo(x: Double, y: Double) extends WSPayload {
    override def toJson: Option[JsValue] = Some(Json.toJson(this))
    override val header = MoveTo.header
  }

  case object StopWatching extends WSPayload {
    val header = "STOP_WATCHING"
  }

  def fromJson(json: String): WSPayload = Json.fromJson[WSMessage](Json.parse(json)).getOrElse(WrongMessage) match {
    case WSMessage(StartWatching.header, Some(payload)) => Json.fromJson[StartWatching](payload).getOrElse(WrongMessage)
    case WSMessage(StopWatching.header, _) => StopWatching
    case WSMessage(MoveTo.header, Some(payload)) => Json.fromJson[MoveTo](payload).getOrElse(WrongMessage)
  }

  def toJson(wsPayload: WSPayload): String = Json.stringify(Json.toJson(WSMessage(wsPayload.header, wsPayload.toJson)))

  object WsMsg {

    def unapply(msg: WSMessage): Option[WSPayload] = msg.header match {

      case StartWatching.header =>
        msg.payload.map(jsVal => Json.fromJson[StartWatching](jsVal).get)

      case Position.header =>
        msg.payload.map(jsVal => Json.fromJson[Position](jsVal).get)

      case StopWatching.header =>
        Some(StopWatching)

      case MoveTo.header =>
        msg.payload.map(jsVal => Json.fromJson[MoveTo](jsVal).get)

      case _ =>
        None
    }

  }

}
