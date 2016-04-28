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
package model

case class RobotSummary(customerFirstName: String,
  customerLastName: String,
  customerPhone: String,
  isDeviceConnected: Boolean,
  activityLevel: ActivityLevel,
  state: Activity,
  controlledBy: Option[String])

sealed trait ActivityLevel { def level: String }
case object NormalActivity extends ActivityLevel { val level = "NORMAL" }
case object WarningActivity extends ActivityLevel { val level = "WARNING" }
case object ErrorActivity extends ActivityLevel { val level = "ERROR" }

sealed trait Activity { def activity: String }
case object MappingLocation extends Activity { val activity = "ML" }
case object AutonomousNavigation extends Activity { val activity = "AN" }
case object Recharging extends Activity { val activity = "RC" }
case object Teleoperation extends Activity { val activity = "TO" }
case object Blocked extends Activity { val activity = "BLOCKED" }

