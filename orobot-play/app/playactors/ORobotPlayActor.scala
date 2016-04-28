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
package playactors

import javax.inject.Inject

import actors.{ Facade, ORobotSupervisor }
import akka.actor.{ Actor, ActorIdentity, ActorLogging, ActorRef, Identify, Props, ReceiveTimeout, Terminated }
import akka.event.LoggingReceive
import env.DefaultEnvironment
import play.api.Configuration

import scala.concurrent.duration._

object ORobotPlayActor {
  val name = "orobotplay"

  def props(conf: Configuration) = Props(classOf[ORobotPlayActor], conf)
}

class ORobotPlayActor @Inject() (conf: Configuration)
    extends Actor
    with ActorLogging {
  log.info("actor path:{}", self.path)

  val system = context.system
  actors.initSystem(system, new DefaultEnvironment {
    override lazy val cfg = conf.underlying
  })

  var facade: Option[ActorRef] = None

  context.setReceiveTimeout(60.seconds)

  val facadePath = s"/user/${ORobotSupervisor.name}/${Facade.name}"
  val actorSelection = context.actorSelection(facadePath)
  log.info("facade send identify!!!")
  actorSelection ! Identify(None)

  private implicit val ec = context.system.dispatcher

  val receive: Receive = PartialFunction.empty[Any, Unit]

  val disconnected: Receive = LoggingReceive {

    case ActorIdentity(_, actorRef) =>
      facade = actorRef
      if (facade.isDefined) {
        log.info("facade is defined")
        context watch actorRef.get
        context become connected
        log.info("facade connected !!!")
      } else {
        log.info("facade is not defined. Retry")
        val actorSelection = context.actorSelection(facadePath)
        context.system.scheduler.scheduleOnce(2.seconds) {
          actorSelection ! Identify(None)
        }
      }

    case ReceiveTimeout =>
      log.info("ReceiveTimeout")
      facade = None
      context become disconnected
      context stop self

    case m => sender ! "NOT READY"
  }

  val connected: Receive = LoggingReceive {

    case Terminated(actorRef) if (actorRef == facade.get) =>
      log.info("Terminated")
      context unwatch actorRef
      facade = None
      context become disconnected

    case m: protocol.RobotsSupervisor.Received => facade.get forward m
    case m: protocol.RobotActor.Received => facade.get forward m

    case m: protocol.RobotRegistrySupervisor.Received => facade.get forward m

  }

  context.become(disconnected)

}

