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
package websocket.actors

import java.util.UUID

import actors.{ Facade, ORobotSupervisor }
import akka.actor.{ Actor, ActorIdentity, ActorLogging, ActorRef, Cancellable, Identify, Props, ReceiveTimeout, Terminated }
import akka.event.LoggingReceive
import models.User
import websocket.protocol._
import protocol.RobotsSupervisor._
import protocol.RobotActor._

import scala.concurrent.duration._

object ORobotWebSocketActor {

  val name = "orobotwebsocket"

  def props(user: User)(out: ActorRef) = Props(new ORobotWebSocketActor(user, out))
}

class ORobotWebSocketActor(user: User, out: ActorRef) extends Actor
    with ActorLogging {
  log.info("actor path:{}", self.path)

  var facade: Option[ActorRef] = None

  var watchedRobot: Option[UUID] = None

  context.setReceiveTimeout(60.seconds)

  val facadePath = s"/user/${ORobotSupervisor.name}/${Facade.name}"
  val actorSelection = context.actorSelection(facadePath)
  log.info("facade send identify!!!")
  actorSelection ! Identify(None)

  var pollingWatchSched: Option[Cancellable] = None

  private implicit val ec = context.system.dispatcher

  override def preStart = context.system.eventStream.subscribe(self, classOf[mqtt.protocol.MqttPayload])

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

    case WsMsg(StartWatching(robotId)) =>
      log.info(s"received message websocket.protocol.StartWatching $robotId")
      if (watchedRobot.isEmpty) {
        watchedRobot = Some(robotId)
        pollingWatchSched = Some(context.system.scheduler.schedule(1.seconds, 15.seconds, facade.get, Cmd4Robot(watchedRobot.get, WatchedBy(user.userID))))
      } else if (watchedRobot.get == robotId) {
        log.info(s"Already watching robot $robotId")
      } else {
        log.error(s"Currently watching robot ${watchedRobot.get}")
      }

    case WsMsg(StopWatching) =>
      log.info("received message websocket.protocol.StopWatching")
      if (watchedRobot.isEmpty) {
        log.info("No robot is currently watched")
      } else {
        watchedRobot = None
        pollingWatchSched.get.cancel()
        pollingWatchSched = None
      }

    case WsMsg(MoveTo(x, y)) =>
      log.info(s"received message websocket.protocol.MoveTo $x $y")
      facade.get ! Cmd4Robot(watchedRobot.get, MovedTo(x, y))

    case mqtt.protocol.Position(robotId, x, y, theta) =>
      log.info(s"received message mqtt.protocol.Position $robotId, $x, $y, $theta")
      if (watchedRobot.isEmpty) {
        log.info("No robot is currently watched")
      } else if (robotId != watchedRobot.get) {
        log.error(s"Currently watching robot ${watchedRobot.get}")
      } else {
        println("#" * 180)
        import websocket.protocol.{ WSMessage, Position }
        out ! WSMessage(Position.header, Position(x, y, theta).toJson)
      }

    case Terminated(actorRef) if (actorRef == facade.get) =>
      log.info("Facade terminated!")
      context unwatch actorRef
      facade = None
      context become disconnected

    case m => log.error(s"Unhandled message $m")
  }

  context.become(disconnected)

}
