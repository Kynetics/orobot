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

import actors.Facade.Env
import akka.actor._
import akka.event.LoggingReceive
import scala.concurrent.duration._

object Facade {

  trait Env {
    def robotsSupervisorPath: String
    def robotRegistrySupervisorPath: String
  }

  val name = "facade"

  def props(env: Env): Props = Props(classOf[Facade], env)

}

class Facade(env: Env)
    extends Actor
    with ActorLogging
    with Stash {
  log.debug("actor path:{}", self.path)

  val receive: Receive = PartialFunction.empty[Any, Unit]

  var robots: Option[ActorRef] = None

  val robotRegistrySupervisorActor = context.actorSelection(env.robotRegistrySupervisorPath)

  val robotsSupervisorActor = context.actorSelection(env.robotsSupervisorPath)
  log.debug("robots supervisor send identify!!!")
  robotsSupervisorActor ! Identify(None)

  val disconnected: Receive = LoggingReceive {

    case ActorIdentity(_, Some(actorRef)) =>
      robots = Some(actorRef)
      context watch actorRef
      unstashAll()
      context become connected
      log.debug("robots supervisor connected !!!")

    case ActorIdentity(_, None) =>
      log.info("robots supervisor is not defined. Retry")
      import scala.concurrent.ExecutionContext.Implicits.global
      context.system.scheduler.scheduleOnce(2.seconds) {
        robotsSupervisorActor ! Identify(None)
      }

    case m =>
      stash()
    //sender ! "NOT READY"
  }

  val connected: Receive = LoggingReceive {

    case Terminated(actorRef) if (actorRef == robots.get) =>
      context unwatch actorRef
      robots = None
      context become disconnected

    case m: protocol.RobotsSupervisor.Received => robots.get forward m

    case m: protocol.RobotRegistrySupervisor.Received => robotRegistrySupervisorActor forward m

  }

  context.become(disconnected)

}