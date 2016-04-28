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

import java.util.UUID

import actors.{ RobotRegistryActor, _ }
import akka.actor.Props
import com.typesafe.config.Config
import elastic.daos.{ EsDao, RobotEventDao }
import elastic.views.{ MqttMessageView, RobotEventView, RobotView }
import elastic.{ DefaultEsComponent, EsComponent }
import model.{ MqttMessageEvent, Robot, RobotEventSummary }
import mqtt.actors.{ MqttConnectorActor, MqttConnectorSupervisor }
import mqtt.protocol.MqttMessage
import net.ceedubs.ficus.Ficus._
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.slf4j.{ Logger, LoggerFactory }
import sim.actors.{ RobotSimActor, RobotSimSupervisor }

trait WebSocketSimEnv
    extends ORobotSupervisor.Env
    with Facade.Env
    with RobotRegistrySupervisor.Env
    with RobotRegistryActor.Env
    with RobotsSupervisor.Env
    with RobotActor.Env
    with MqttConnectorActor.Env
    with RobotSimSupervisor.Env {

  override lazy val robotRegistrySupervisorProps: Props = RobotRegistrySupervisor.props(this)

  override lazy val robotsSupervisorProps: Props = RobotsSupervisor.props(this)

  override lazy val facadeProps: Props = Facade.props(this)

  override lazy val robotsSupervisorPath: String = s"/user/${ORobotSupervisor.name}/${RobotsSupervisor.name}"

  override lazy val robotRegistryActorProps: Props = RobotRegistryActor.props(this)

  override lazy val robotActorProps: Props = RobotActor.props(this)

  override lazy val robotRegistrySupervisorPath: String = s"/user/${ORobotSupervisor.name}/${RobotRegistrySupervisor.name}"

  override lazy val mqttConnectorSupervisorProps: Props = MqttConnectorSupervisor.props(this)

  override lazy val mqttConnectorSupervisorPath: String = s"/user/${ORobotSupervisor.name}/${MqttConnectorSupervisor.name}"

  override lazy val robotSimActorProps: Props = RobotSimActor.props

  @transient lazy val esComponent = new DefaultEsComponent()

  @transient override lazy val robotDao: EsDao[Robot] =
    new EsDao[Robot] with EsComponent with RobotView {
      override val client = esComponent.client
    }

  @transient override lazy val mqttMessageDao: EsDao[MqttMessageEvent] =
    new EsDao[MqttMessageEvent] with EsComponent with MqttMessageView {
      override val client = esComponent.client
    }

  @transient override lazy val robotEventDao: RobotEventDao[RobotEventSummary] =
    new RobotEventDao[RobotEventSummary] with EsComponent with RobotEventView {
      override val client = esComponent.client
    }

  private val log: Logger = LoggerFactory.getLogger(WebSocketSimEnv.this.getClass)
  def cfg: Config

  override def mqttClient = {
    log.debug("Initializing mqtt client")
    val brokerUrl = cfg.as[String]("mosquitto.broker.url")
    log.debug("brokerUrl {}", brokerUrl)
    val client = new MqttClient(brokerUrl, UUID.randomUUID().toString, new MemoryPersistence())
    client
  }
}