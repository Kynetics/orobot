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
package sim.actors

import akka.actor.{ Actor, ActorLogging, Props }
import mqtt.actors.MqttConnectorSupervisor
import RobotSimSupervisor.Env

object RobotSimSupervisor {

  trait Env {
    def mqttConnectorSupervisorProps: Props
    def robotSimActorProps: Props
  }

  val name = "orobotsim"

  def props(env: Env) = Props(classOf[RobotSimSupervisor], env)

}

class RobotSimSupervisor(env: Env)
    extends Actor
    with ActorLogging {

  log.debug("starting actor [path:{}]", self.path)

  context.actorOf(env.mqttConnectorSupervisorProps, MqttConnectorSupervisor.name)
  Thread.sleep(500)
  context.actorOf(env.robotSimActorProps, RobotSimActor.name)

  val receive: Receive = Actor.emptyBehavior

}
