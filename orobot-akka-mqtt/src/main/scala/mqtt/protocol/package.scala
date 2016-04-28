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
package mqtt

import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream
import java.util.{ Base64, UUID }
import javax.imageio.ImageIO

import play.api.libs.json.Reads._
import play.api.libs.json._

package object protocol {

  sealed trait Priority { def level: Int }
  case object PriorityHigh extends Priority { val level = 1 }
  case object PriorityNormal extends Priority { val level = 2 }
  case object PriorityLow extends Priority { val level = 3 }

  def parse(s: Int): Priority = s match {
    case 1 => PriorityHigh
    case 2 => PriorityNormal
    case 3 => PriorityLow
    case _ => PriorityLow
  }

  implicit val priorityReads = (__).read[Int].map { p => parse(p) }

  implicit val priorityWrites = new Writes[Priority] {
    def writes(p: Priority) = Json.toJson(p.level)
  }

  trait MqttPayload {
    def toJson: Option[JsValue] = None
    def header: String
    def priority: Option[Priority] = None
  }

  case object WrongMessage extends MqttPayload {
    override val header = "WRONG_MSG"
  }

  case object KeepAlive extends MqttPayload {
    override val header = "KEEP_ALIVE"
  }

  case class MqttMessage(header: String, payload: Option[JsValue])

  object MqttMessage {
    implicit val mqttMessageFormat = Json.format[MqttMessage]
  }

  object Configure {
    implicit val configureFormat = Json.format[Configure]
    val header = "CONFIGURE"
  }

  case class Configure(messageUuid: UUID, locationMap: String) extends MqttPayload {
    override def toJson: Option[JsValue] = Some(Json.toJson(this))
    override val header = Configure.header
    def getPng = ImageIO.read(new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder.decode(locationMap))))
  }

  case object Alive extends MqttPayload {
    override val header = "ALIVE"
  }

  object Ack {
    implicit val configureFormat = Json.format[Ack]
    val header = "ACK"
  }

  case class Ack(messageUuid: Option[UUID] = None) extends MqttPayload {
    override def toJson: Option[JsValue] = Some(Json.toJson(this))
    override val header = Ack.header
  }

  object WatchedBy {
    implicit val watchedByFormat = Json.format[WatchedBy]
    val header = "WATCHED_BY"
  }

  case class WatchedBy(operatorId: UUID) extends MqttPayload {
    override def toJson: Option[JsValue] = Some(Json.toJson(this))
    override val header = WatchedBy.header
  }

  object MovedTo {
    implicit val watchedByFormat = Json.format[MovedTo]
    val header = "MOVETO"
  }

  case class MovedTo(x: Double, y: Double) extends MqttPayload {
    override def toJson: Option[JsValue] = Some(Json.toJson(this))
    override val header = MovedTo.header
  }

  object Position {
    implicit val positionFormat = Json.format[Position]
    val header = "POSITION"
  }

  case class Position(robotId: UUID, x: Double, y: Double, theta: Double) extends MqttPayload {
    override def toJson: Option[JsValue] = Some(Json.toJson(this))
    override val header = Position.header
  }

  object FallenPerson {
    implicit val fallenPersonFormat = Json.format[FallenPerson]
    val header = "FALLEN_PERSON"
    val priority = PriorityHigh
  }

  case class FallenPerson(override val priority: Option[Priority] = Some(PriorityHigh), messageUuid: UUID, robotId: UUID, x: Double, y: Double, timestamp: Long) extends MqttPayload {
    override def toJson: Option[JsValue] = Some(Json.toJson(this))
    override val header = FallenPerson.header
  }

  object DetectedPerson {
    implicit val detectedPersonFormat = Json.format[DetectedPerson]
    val header = "DETECTED_PERSON"
    val priority = PriorityNormal
  }

  case class DetectedPerson(override val priority: Option[Priority] = Some(PriorityNormal), messageUuid: UUID, robotId: UUID, x: Double, y: Double, personId: Int, timestamp: Long) extends MqttPayload {
    override def toJson: Option[JsValue] = Some(Json.toJson(this))
    override val header = DetectedPerson.header
  }

  object RecognizedPerson {
    implicit val recognizedPersonFormat = Json.format[RecognizedPerson]
    val header = "RECOGNIZED_PERSON"
    val priority = PriorityNormal
  }

  case class RecognizedPerson(override val priority: Option[Priority] = Some(PriorityNormal), messageUuid: UUID, robotId: UUID, nome: String, personId: Int, timestamp: Long) extends MqttPayload {
    override def toJson: Option[JsValue] = Some(Json.toJson(this))
    override val header = RecognizedPerson.header
  }

  object BatteryLevel {
    implicit val recognizedPersonFormat = Json.format[BatteryLevel]
    val header = "BATTERY_LEVEL"
    val priority = PriorityHigh
  }

  case class BatteryLevel(override val priority: Option[Priority] = Some(PriorityHigh), messageUuid: UUID, robotId: UUID, timestamp: Long) extends MqttPayload {
    override def toJson: Option[JsValue] = Some(Json.toJson(this))
    override val header = BatteryLevel.header
  }

  def fromJson(json: String): MqttPayload = Json.fromJson[MqttMessage](Json.parse(json)).getOrElse(WrongMessage) match {
    case MqttMessage(KeepAlive.header, _) => KeepAlive
    case MqttMessage(Alive.header, _) => Alive
    case MqttMessage(Configure.header, Some(payload)) => Json.fromJson[Configure](payload).getOrElse(WrongMessage)
    case MqttMessage(Ack.header, None) => new Ack()
    case MqttMessage(Ack.header, Some(payload)) => Json.fromJson[Ack](payload).getOrElse(WrongMessage)
    case MqttMessage(WatchedBy.header, Some(payload)) => Json.fromJson[WatchedBy](payload).getOrElse(WrongMessage)
    case MqttMessage(Position.header, Some(payload)) => Json.fromJson[Position](payload).getOrElse(WrongMessage)
    case MqttMessage(FallenPerson.header, Some(payload)) => Json.fromJson[FallenPerson](payload).getOrElse(WrongMessage)
    case MqttMessage(DetectedPerson.header, Some(payload)) => Json.fromJson[DetectedPerson](payload).getOrElse(WrongMessage)
    case MqttMessage(RecognizedPerson.header, Some(payload)) => Json.fromJson[RecognizedPerson](payload).getOrElse(WrongMessage)
    case MqttMessage(BatteryLevel.header, Some(payload)) => Json.fromJson[BatteryLevel](payload).getOrElse(WrongMessage)
    case MqttMessage(MovedTo.header, Some(payload)) => Json.fromJson[MovedTo](payload).getOrElse(WrongMessage)
  }

  def toJson(mqttPayload: MqttPayload): String = Json.stringify(Json.toJson(MqttMessage(mqttPayload.header, mqttPayload.toJson)))

}
