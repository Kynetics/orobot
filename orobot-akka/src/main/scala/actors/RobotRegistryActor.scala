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

import actors.ALODProxyActor.{ Message => AlodpMessage }
import actors.RobotActor.{ DeregistrationConfirmed, RegistrationConfirmed }
import actors.RobotRegistryActor.Env
import akka.actor.{ ActorLogging, Props }
import akka.event.LoggingReceive
import akka.persistence.{ PersistentActor, RecoveryCompleted, SnapshotOffer }
import elastic.daos.{ EsDao, PageRequest, PagedResult, RobotEventDao }
import model._
import persistence.RobotRegistryPersistence
import mqtt.protocol.MqttMessage

import scala.concurrent.ExecutionContext.Implicits.global

object RobotRegistryActor {

  trait Env {
    def robotEventDao: RobotEventDao[RobotEventSummary]
    def mqttMessageDao: EsDao[MqttMessageEvent]
  }

  val name = "registry"

  def props(env: Env) = Props(classOf[RobotRegistryActor], env)

  sealed trait Received
  case class RegisterRobot(uuid: UUID, customer: Customer) extends Received
  case class DeregisterRobot(uuid: UUID) extends Received

}

class RobotRegistryActor(val env: Env)
    extends PersistentActor
    with ActorLogging
    with RobotRegistryPersistence {
  log.debug("actor path:{}", self.path)
  import RobotRegistryActor._
  import RobotRegistryPersistence.RobotRegistryEvents._
  import RobotRegistryPersistence.State

  val persistenceId: String = classOf[RobotRegistryActor].getSimpleName

  override var state = State()

  def logUnhandled(msg: Any, receive: String): Unit = log.warning(s"received hunandled message $msg in $receive")

  val receiveCommand: Receive = LoggingReceive {

    case AlodpMessage(deliveryId, RegisterRobot(uuid: UUID, customer: Customer)) =>
      log.debug(s"command RegisterRobot for customer=$customer")
      val snd = sender()
      persist(RobotRegistered(uuid, customer)) { evt =>
        updateState(evt)
        snd ! AlodpMessage(deliveryId, RegistrationConfirmed)
      }

    case AlodpMessage(deliveryId, DeregisterRobot(uuid: UUID)) =>
      log.debug(s"command DeregisterRobot for uuid=$uuid")
      val snd = sender()
      persist(RobotDeregistered(uuid)) { evt =>
        updateState(evt)
        snd ! AlodpMessage(deliveryId, DeregistrationConfirmed)
      }

    case protocol.RobotRegistryActor.RobotsList(page, size, order) =>
      log.debug(s"command RobotRegistryActor.RobotsList with page=$page size=$size order=${order.getOrElse("default ordering")}")
      val snd = sender()
      val offset = (page - 1) * size
      val totalResults = state.robots.size
      val results = state.robots.drop(offset).take(size).toSeq //TODO order
      snd ! protocol.RobotRegistryActor.RobotListResult(totalResults, results)

    case protocol.RobotRegistryActor.EventsList(page, size) =>
      log.debug(s"command RobotRegistryActor.EventsList with page=$page size=$size")
      val snd = sender()
      val result = env.robotEventDao.all(PageRequest(page, size))
      result.map {
        case r: PagedResult[RobotEventSummary] =>
          snd ! protocol.RobotRegistryActor.EventListResult(r)
      }.recover {
        case ex: Exception =>
          log.info(s"Error during list events $ex")
          snd ! protocol.RobotRegistryActor.EventListResult(PagedResult(0, PageRequest(page, size), None))
      }

    case protocol.RobotRegistryActor.RobotEvents(robotId, page, size) =>
      log.debug(s"command RobotRegistryActor.RobotEvents with robotId=$robotId page=$page size=$size")
      val snd = sender()
      val result = env.robotEventDao.allByRobotId(robotId.toString, PageRequest(page, size))
      result.map {
        case r: PagedResult[RobotEventSummary] =>
          snd ! protocol.RobotRegistryActor.RobotEventsResult(r)
      }.recover {
        case ex: Exception =>
          log.info(s"Error during list events $ex")
          snd ! protocol.RobotRegistryActor.RobotEventsResult(PagedResult(0, PageRequest(page, size), None))
      }

    case protocol.RobotRegistryActor.EventDetail(messageId) =>
      log.debug(s"command RobotRegistryActor.EventDetail with messageId=$messageId")
      val snd = sender()
      val result = env.mqttMessageDao.get(messageId)
      result.map {
        r => snd ! protocol.RobotRegistryActor.EventDetailResult(r)
      }.recover {
        case ex: Exception =>
          log.info(s"Error during list events $ex")
          snd ! protocol.RobotRegistryActor.EventDetailResult(None)
      }

    case _ => log.error("receiveCommand::Unhandled message")
  }

  val receiveRecover: Receive = LoggingReceive {

    case event: Event =>
      log.info("recovering")
      updateState(event)

    case SnapshotOffer(_, snapshot: State) => state = snapshot

    case RecoveryCompleted =>
      log.info("RecoveryCompleted")

    case _ => log.error("receiveRecover::Unhandled message")
  }

  def updateState(evt: Event) =
    state = state.updated(evt)

}
