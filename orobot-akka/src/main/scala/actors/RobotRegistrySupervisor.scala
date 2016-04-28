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

import actors.ALODProxyActor.Message
import actors.RobotRegistryActor.Received
import actors.RobotRegistrySupervisor.Env
import akka.actor.{ Actor, ActorLogging, Props }
import akka.event.LoggingReceive
import protocol.RobotRegistrySupervisor.Cmd4Registry

object RobotRegistrySupervisor {

  trait Env {
    def robotRegistryActorProps: Props
  }

  val name = "registry"

  def props(env: Env) = Props(classOf[RobotRegistrySupervisor], env)

}

class RobotRegistrySupervisor(env: Env)
    extends Actor
    with ActorLogging {
  log.debug("actor path:{}", self.path)
  val registry = context.actorOf(env.robotRegistryActorProps, RobotRegistryActor.name)

  val receive: Receive = LoggingReceive {

    case m: Received => registry forward m

    case m @ Message(_, r: Received) => registry forward m

    case Cmd4Registry(cmd) => registry forward cmd

  }
}
