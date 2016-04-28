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

import java.util.UUID

import akka.actor.ActorSystem
import com.typesafe.config.Config
import mqtt.actors.MqttConnectorActor.Env
import net.ceedubs.ficus.Ficus._
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence
import org.slf4j.{ Logger, LoggerFactory }

package object actors {

  def initSystem(system: ActorSystem, env: Env): Unit = {
    system.actorOf(MqttConnectorSupervisor.props(env), MqttConnectorSupervisor.name)
  }

  trait DefaultEnv extends Env {
    override val topicSuffixes = MqttConnectorActor.fromDeviceTopicSuffix
    private val log: Logger = LoggerFactory.getLogger(DefaultEnv.this.getClass)
    def cfg: Config

    override def mqttClient = {
      log.debug("Initializing mqtt client")
      val brokerUrl = cfg.as[String]("mosquitto.broker.url")
      log.debug("brokerUrl {}", brokerUrl)
      val persistence = new MqttDefaultFilePersistence(cfg.getAs[String]("mqtt.default.file.persistence").getOrElse(System.getProperty("user.dir")))
      val client = new MqttClient(brokerUrl, UUID.randomUUID().toString, persistence)
      client
    }
  }

}
