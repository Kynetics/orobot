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

import java.util.UUID

import actors.RobotsSupervisor.Env
import akka.actor._
import akka.event.LoggingReceive
import protocol.RobotsSupervisor.Cmd4Robot

object RobotsSupervisor {

  trait Env {
    def robotActorProps: Props
  }

  val name = "robots"

  def props(env: Env) = Props(classOf[RobotsSupervisor], env)

}

class RobotsSupervisor(env: Env)
    extends Actor
    with ActorLogging {
  log.debug("actor path:{}", self.path)

  val receive: Receive = LoggingReceive {

    case Cmd4Robot(robotId, cmd) => robotActor(robotId) forward cmd

  }

  protected def robotActor(uuid: UUID): ActorRef = {
    val name = RobotActor.name(uuid)
    context.child(name) match {
      case Some(actRef) => actRef
      case None => context.actorOf(env.robotActorProps, name)
    }
  }
}
