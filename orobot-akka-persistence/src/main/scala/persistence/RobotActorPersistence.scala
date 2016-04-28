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
package persistence

import java.util.UUID

import model.{ Customer, MqttMessageEvent }
import mqtt.protocol.MqttMessage

object RobotActorPersistence {

  object RobotActorEvents {

    sealed trait Event
    case class RobotCreated(customer: Customer) extends Event
    case object RobotRemoved extends Event
    case class RobotConfigured(locationMap: String) extends Event
    case object AliveReceived extends Event
    case object AliveMissed extends Event
    case class MqttConnected(isConnected: Boolean) extends Event
    case class RobotEvent(mqttMessageEvent: MqttMessageEvent) extends Event
  }

  case class State(
      uuid: UUID,
      customer: Option[Customer] = None,
      configured: Boolean = false,
      @transient missedAlive: Int = Int.MaxValue,
      @transient mqttConnected: Boolean = false,
      locationMap: Option[String] = None,
      removed: Boolean = false) {
    import RobotActorEvents._
    def updated(evt: Event) = (evt: @unchecked) match {
      case RobotCreated(customer) => copy(customer = Some(customer))
      case RobotConfigured(locationMap) => copy(configured = true, missedAlive = 0, locationMap = Some(locationMap))
      case AliveReceived => copy(missedAlive = 0)
      case AliveMissed => copy(missedAlive = missedAlive + 1)
      case MqttConnected(isConnected) => copy(mqttConnected = isConnected, missedAlive = 0)
      case RobotRemoved => copy(removed = true)
    }
  }
}

trait RobotActorPersistence {
  import RobotActorPersistence.State
  var state: State
}
