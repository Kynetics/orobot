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
package mqtt.actors

import java.util.UUID

import akka.actor.{ Actor, ActorLogging, ActorRef, Props, Terminated }
import akka.event.LoggingReceive
import mqtt.actors.MqttConnectorActor.Env
import mqtt.protocol.{ MqttPayload, _ }
import net.ceedubs.ficus.Ficus._
import org.eclipse.paho.client.mqttv3.{ MqttMessage, _ }

object MqttConnectorActor {

  trait Env {
    def mqttClient: IMqttClient
    def topicSuffixes: TopicSuffixes
  }

  def name = "mqttconnector"

  def props(env: Env) = Props(classOf[MqttConnectorActor], env)

  trait Received
  case object ConnectToSystem extends Received
  case object DisconnectFromSystem extends Received
  case class Register(uuid: UUID) extends Received
  case class Unregister(uuid: UUID) extends Received
  case class PayloadForRobot(uuid: UUID, payload: MqttPayload) extends Received

  trait Sent
  case object Registered extends Sent

  case class TopicSuffixes(in: String, out: String) {
    def swapped = new TopicSuffixes(out, in)
  }

  val fromDeviceTopicSuffix = new TopicSuffixes("fromDevice", "toDevice")

  def topicIn(uuid: UUID, suffix: TopicSuffixes) = s"${uuid.toString}_${suffix.in}"
  def topicOut(uuid: UUID, suffix: TopicSuffixes) = s"${uuid.toString}_${suffix.out}"
}

class MqttConnectorActor(env: Env)
    extends Actor
    with ActorLogging {
  import MqttConnectorActor._
  val cfg = context.system.settings.config
  log.debug("actor path:{}", self.path)

  val jsonEncoding = cfg.getAs[String]("mqtt.encoding").getOrElse("UTF-8")

  var refByIdMap: Map[UUID, ActorRef] = Map.empty
  var idByRefMap: Map[ActorRef, UUID] = Map.empty

  val _topicSuffixes = env.topicSuffixes

  def uuidFromTopic(topic: String) = UUID.fromString(topic.substring(0, topic.indexOf("_")))

  val mqttCallback = new MqttCallback() {

    def connectionLost(cause: Throwable): Unit = {
      log.error(cause, "Connection Lost to MQTT")
      //send msg con eccezione
      throw cause
    }

    def deliveryComplete(token: IMqttDeliveryToken): Unit = {
      log.info("Mqtt delivery complete")
      //not implemented in this actor
    }

    def messageArrived(topic: String, message: MqttMessage): Unit = {
      log.debug(s"message [$message] arrived on actor [$self] on topic $topic")
      val msgStr = new String(message.getPayload, jsonEncoding)
      val msg4Mqtt = fromJson(msgStr)
      val topicUuid = uuidFromTopic(topic)
      self ! PayloadForRobot(topicUuid, msg4Mqtt)
    }
  }

  val mqttClient = env.mqttClient
  mqttClient.setCallback(mqttCallback)

  private def disconnect = {
    if (mqttClient.isConnected) mqttClient.disconnect()
    mqttClient.close()
  }

  override def postStop(): Unit = {
    disconnect
  }

  override def preStart(): Unit = {
    log.info("Starting mqtt connection and callback")
    mqttClient.connect()
  }

  val receive: Receive = LoggingReceive {

    case Register(uuid: UUID) =>
      log.info("Received Register")
      val snd = sender()
      refByIdMap += uuid -> sender()
      idByRefMap += sender() -> uuid
      context watch snd
      mqttClient.subscribe(topicIn(uuid, _topicSuffixes))
      snd ! Registered
      log.debug(s"robot $uuid registered on mqtt !!!")

    case Terminated(actorRef) =>
      log.info("Received Terminated")
      context unwatch actorRef
      val id = idByRefMap(actorRef)
      refByIdMap -= id
      idByRefMap -= actorRef
      mqttClient.unsubscribe(topicIn(id, _topicSuffixes))

    case PayloadForRobot(uuid, payload) =>
      log.debug(s"received mqtt msg $payload")
      if (refByIdMap.contains(uuid))
        refByIdMap(uuid) ! payload
      else
        log.error(s"No actorRef for id $uuid")

    case p: MqttPayload =>
      val snd = sender()
      if (idByRefMap.contains(snd)) {
        val tout = topicOut(idByRefMap(snd), _topicSuffixes)
        log.debug(s"send mqtt msg $p to topic $tout")
        mqttClient.publish(tout, toJson(p).getBytes(jsonEncoding), 1, false)
      } else
        log.error(s"Robot $snd not registerd")

    case _ => log.error("Unhandled message")
  }

}
