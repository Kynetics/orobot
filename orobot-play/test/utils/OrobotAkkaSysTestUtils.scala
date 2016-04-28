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
package utils

import java.io.IOException
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.{ CountDownLatch, TimeUnit, TimeoutException }

import actors.ORobotSupervisor
import akka.actor.ActorSystem
import com.typesafe.config.{ ConfigFactory, ConfigValueFactory }
import elastic.DefaultEsComponent
import mqtt.actors.MqttConnectorActor
import sim.actors.RobotSimSupervisor
import websockets.WebSocketSimEnv

import scala.sys.process.{ Process, ProcessLogger }
import scala.util.{ Random, Try }

class OrobotAkkaSysTestUtils(override val mqttPort: Int, val robotId: String) extends OrobotTestUtils(mqttPort) with WebSocketSimEnv {

  var suffixIndex = 0

  override def cfg = ConfigFactory.empty().withValue(
    "elastic.local.path.home",
    ConfigValueFactory.fromAnyRef(esDataDirStr)
  ).withValue(
      "elastic.local.path.data",
      ConfigValueFactory.fromAnyRef(esDataDirStr + "/data")
    )
    .withValue(
      "akka.persistence.journal.plugin",
      ConfigValueFactory.fromAnyRef("inmemory-journal"))
    .withValue(
      "akka.persistence.snapshot-store.plugin",
      ConfigValueFactory.fromAnyRef("inmemory-snapshot-store"))
    .withValue(
      "mosquitto.broker.url",
      ConfigValueFactory.fromAnyRef(s"tcp://localhost:$mqttPort"))
    .withValue("akka.actor.serialize-creators", ConfigValueFactory.fromAnyRef("off"))
    .withValue("akka.actor.serialize-messages", ConfigValueFactory.fromAnyRef("off"))
    .withValue("sim.robot.id", ConfigValueFactory.fromAnyRef(robotId))
    .withValue("sim.robot.registerAtStartup", ConfigValueFactory.fromAnyRef(true))
    .withValue("sim.env.scale", ConfigValueFactory.fromAnyRef(0.042))
    .withValue("sim.env.wallPath", ConfigValueFactory.fromAnyRef("""
                                                                   | m 0,322 0,-240 100,0 0,-60 110,0 0,-20 440,0 0,70
                                                                   | -80,0 0,10 130,0 0,240 -170,0 0,-250 -10,0 0,250
                                                                   | -130,0 0,-240 80,0 0,-10 -90,0 0,250 -60,0 0,-170
                                                                   | -20,0 0,-70 50,0 0,-10 -80,0 0,10 20,0 0,120 20,0
                                                                   | 0,120 -100,0 0,-240 20,0 0,-10 -50,0 0,10 20,0 0,240 z"""))
    .withValue("sim.env.walkPath", ConfigValueFactory.fromAnyRef("""
                                                                   | m 20,292 125,-245 105,0 0,255 0,-255 115,0 0,240
                                                                   | 0,-240 135,0 0,235 0,-235 55,0 0,65 125,190
                                                                   | -125,-190 0,-65 -410,0 z"""))

  @transient override lazy val esComponent = new DefaultEsComponent(Some(cfg))

  def startSystem: ActorSystem = {
    val system = ActorSystem("ORobotSimSpec", cfg)
    initSystem(system, this)
    system
  }

  def initSystem(system: ActorSystem, env: WebSocketSimEnv): Unit = {
    system.actorOf(ORobotSupervisor.props(env), ORobotSupervisor.name)
    system.actorOf(RobotSimSupervisor.props(env), RobotSimSupervisor.name)
  }

  override def topicSuffixes =
    if (suffixIndex == 0) {
      suffixIndex = suffixIndex + 1
      MqttConnectorActor.fromDeviceTopicSuffix.swapped
    } else if (suffixIndex == 1) {
      suffixIndex = suffixIndex + 1
      MqttConnectorActor.fromDeviceTopicSuffix
    } else {
      throw new IllegalStateException
    }
}
