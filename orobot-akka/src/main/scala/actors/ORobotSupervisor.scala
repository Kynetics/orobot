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
package actors

import actors.ORobotSupervisor.Env
import akka.actor.{ Actor, ActorLogging, Props }
import mqtt.actors.MqttConnectorSupervisor

object ORobotSupervisor {

  trait Env {
    def mqttConnectorSupervisorProps: Props
    def robotRegistrySupervisorProps: Props
    def robotsSupervisorProps: Props
    def facadeProps: Props
  }

  val name = "orobot"

  def props(env: Env) = Props(classOf[ORobotSupervisor], env)

}

class ORobotSupervisor(env: Env)
    extends Actor
    with ActorLogging {

  log.debug("starting actor [path:{}]", self.path)

  context.actorOf(env.mqttConnectorSupervisorProps, MqttConnectorSupervisor.name)
  context.actorOf(env.robotRegistrySupervisorProps, RobotRegistrySupervisor.name)
  context.actorOf(env.robotsSupervisorProps, RobotsSupervisor.name)
  context.actorOf(env.facadeProps, Facade.name)

  val receive: Receive = Actor.emptyBehavior
}
