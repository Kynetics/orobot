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

import akka.actor.{ ExtensionId, Actor, ActorSystem }

import scala.concurrent.duration.{ Duration, FiniteDuration }
import scala.util.Try

package object actors {

  def initSystem(system: ActorSystem, env: ORobotSupervisor.Env): Unit = {
    system.actorOf(ORobotSupervisor.props(env), ORobotSupervisor.name)
  }
}
