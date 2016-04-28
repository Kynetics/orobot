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

import model._

object RobotRegistryPersistence {

  object RobotRegistryEvents {
    sealed trait Event
    case class RobotRegistered(uuid: UUID, customer: Customer) extends Event
    case class RobotDeregistered(uuid: UUID) extends Event
  }

  case class State(robots: Map[UUID, RobotSummary] = Map.empty) {
    import RobotRegistryEvents._
    def updated(evt: Event) = evt match {
      case RobotRegistered(uuid, customer) =>
        val robotSummary = new RobotSummary(customer.firstName,
          customer.lastName,
          customer.phone,
          true,
          NormalActivity,
          Recharging,
          None)
        copy(robots = robots + (uuid -> robotSummary))
      case RobotDeregistered(uuid) =>
        copy(robots = robots - uuid)
    }
  }
}

trait RobotRegistryPersistence {
  import RobotRegistryPersistence.State
  var state: State
}

