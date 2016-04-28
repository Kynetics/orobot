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
package json

import java.util.UUID

import model.{ Activity, ActivityLevel, RobotSummary }
import play.api.libs.json._

trait RobotSummaryJson {

  implicit object activityLevelWrites extends Writes[ActivityLevel] {
    def writes(a: ActivityLevel) = Json.toJson(a.level)
  }

  implicit object activityWrites extends Writes[Activity] {
    def writes(c: Activity) = Json.toJson(c.activity)
  }

  implicit val robotSummaryWrites: Writes[RobotSummary] = new Writes[RobotSummary] {
    def writes(rs: RobotSummary): JsObject = Json.obj(
      "customerFirstName" -> rs.customerFirstName,
      "customerLastName" -> rs.customerLastName,
      "customerPhone" -> rs.customerPhone,
      "isDeviceConnected" -> rs.isDeviceConnected,
      "activityLevel" -> rs.activityLevel,
      "activity" -> rs.state,
      "controlledBy" -> rs.controlledBy
    )
  }

  implicit val writer = new Writes[(UUID, RobotSummary)] {
    def writes(t: (UUID, RobotSummary)): JsObject = Json.obj(
      "UUID" -> t._1,
      "robotSummary" -> t._2
    )
  }

}
