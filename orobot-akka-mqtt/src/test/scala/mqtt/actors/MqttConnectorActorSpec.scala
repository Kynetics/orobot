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

import java.time.{ Clock, Instant }
import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit._
import mqtt.actors.MqttConnectorActor.{ Register, Registered }
import mqtt.protocol.{ Ack, Configure, FallenPerson }
import org.eclipse.paho.client.mqttv3.{ IMqttClient, MqttCallback, MqttMessage }
import org.scalamock.function.FunctionAdapter4
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ Matchers, WordSpecLike }
import play.api.libs.json.Json

class MqttConnectorActorSpec extends TestKit(ActorSystem("MqttConnectorActorSpec")) with WordSpecLike with MockFactory with ImplicitSender with Matchers {

  "an MqttConnectorActor" should {

    val uuid = UUID.randomUUID()
    var callback: MqttCallback = null
    var actorRef: TestActorRef[MqttConnectorActor] = null
    val probe = TestProbe()
    val mqttClient = mock[IMqttClient]

    "call setCallback and connect on mqttClient on Startup" in {
      inSequence {
        (mqttClient.setCallback _).expects(*).once().onCall((c: MqttCallback) => callback = c)
        (mqttClient.connect _: () => Unit).expects().once()
      }
      actorRef = TestActorRef[MqttConnectorActor](new MqttConnectorActor(fakeEnv(mqttClient)))
    }

    "register a robot actor ref on receiving Register message" in {
      inSequence {
        (mqttClient.subscribe _: String => Unit).expects(MqttConnectorActor.topicIn(uuid, MqttConnectorActor.fromDeviceTopicSuffix)).once()
      }
      actorRef tell (Register(uuid), probe.ref)
      actorRef.underlyingActor.refByIdMap should contain(uuid -> probe.ref)
      actorRef.underlyingActor.idByRefMap should contain(probe.ref -> uuid)
      probe.expectMsg(Registered)
    }

    "forward an mqtt message from broker to akka robot message" in {
      val msg = Configure(UUID.randomUUID(), "test-info")
      val bytes = Json.stringify(Json.toJson(mqtt.protocol.MqttMessage(Configure.header, msg.toJson))).getBytes("UTF-8")
      callback.messageArrived(MqttConnectorActor.topicIn(uuid, MqttConnectorActor.fromDeviceTopicSuffix), new MqttMessage(bytes))
      probe.expectMsg(msg)
    }

    "forward an akka robot message to mqtt broker" in {
      val ack = Ack(Some(uuid))
      val bytes = Json.stringify(Json.toJson(mqtt.protocol.MqttMessage("ACK", ack.toJson))).getBytes("UTF-8")
      val uuidStr = MqttConnectorActor.topicOut(uuid, MqttConnectorActor.fromDeviceTopicSuffix)
      (mqttClient.publish(_: String, _: Array[Byte], _: Int, _: Boolean)).expects(new FunctionAdapter4({
        case (`uuidStr`, bbs, 1, false) => bbs.toList == bytes.toList
        case _ => false
      }))
      actorRef tell (ack, probe.ref)
    }

    "forward an mqtt event message from broker to akka robot message" in {
      val msg = FallenPerson(Some(mqtt.protocol.PriorityHigh), UUID.randomUUID(), UUID.randomUUID(), 1.0, 1.0, Instant.now(Clock.systemUTC()).toEpochMilli)
      val bytes = Json.stringify(Json.toJson(mqtt.protocol.MqttMessage(FallenPerson.header, msg.toJson))).getBytes("UTF-8")
      callback.messageArrived(MqttConnectorActor.topicIn(uuid, MqttConnectorActor.fromDeviceTopicSuffix), new MqttMessage(bytes))
      probe.expectMsg(msg)
    }

    "remove akka robot from maps on robot termination" in {
      inSequence {
        (mqttClient.unsubscribe _: String => Unit).expects(MqttConnectorActor.topicIn(uuid, MqttConnectorActor.fromDeviceTopicSuffix)).once()
      }
      system.stop(probe.ref)
      Thread.sleep(100)
      actorRef.underlyingActor.refByIdMap shouldNot contain(uuid -> probe.ref)
      actorRef.underlyingActor.idByRefMap shouldNot contain(probe.ref -> uuid)
    }

  }

  def fakeEnv(iMqttClient: => IMqttClient): MqttConnectorActor.Env = {
    new Object with MqttConnectorActor.Env {
      override def mqttClient: IMqttClient = iMqttClient
      override def topicSuffixes = MqttConnectorActor.fromDeviceTopicSuffix
    }
  }

}
