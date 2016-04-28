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
import java.util.UUID

import elastic.daos.PagedResult
import elastic.views.MqttMessageView
import model._
import mqtt.protocol.MqttMessage

import scala.concurrent.Future

package object protocol {

  trait ErrorMessage

  object RobotsSupervisor {

    trait Received
    case class Cmd4Robot(robotId: UUID, cmd: RobotActor.Received) extends Received
  }

  object RobotRegistrySupervisor {
    trait Received
    case class Cmd4Registry(cmd: RobotRegistryActor.Received) extends Received
  }

  object RobotActor {

    trait Received
    case class Create(data: Customer) extends Received
    case object Get extends Received
    case object Remove extends Received
    case class WatchedBy(operatorId: UUID) extends Received
    case class MovedTo(x: Double, y: Double) extends Received

    trait Sent
    case object Unregistered extends Sent
    case class Initialized(robot: Robot) extends Sent

    trait Error extends ErrorMessage
    case object NotInitialized extends Sent with Error

  }

  object RobotRegistryActor {
    trait Received
    case class RobotsList(page: Int, offset: Int, orderBy: Option[Seq[Int]]) extends Received
    case class EventsList(page: Int, offset: Int) extends Received
    case class RobotEvents(robotId: UUID, page: Int, offset: Int) extends Received
    case class EventDetail(messageId: UUID) extends Received

    trait Sent
    case class RobotListResult(total: Long, items: Seq[(UUID, RobotSummary)]) extends Sent
    case class EventListResult(result: PagedResult[RobotEventSummary]) extends Sent
    case class RobotEventsResult(result: PagedResult[RobotEventSummary]) extends Sent
    case class EventDetailResult(result: Option[MqttMessageEvent]) extends Sent
  }
}
