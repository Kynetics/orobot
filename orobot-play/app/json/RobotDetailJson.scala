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

import elastic.json.CustomerJson
import persistence.RobotActorPersistence._
import play.api.libs.json.{ JsObject, Json, Writes }

case class RobotDetail(state: State, rtcSoketUrl: String)

trait RobotDetailJson extends RobotStateJson {
  implicit val robotDetailFormat = Json.format[RobotDetail]

  implicit val robotDetailWrites: Writes[RobotDetail] = new Writes[RobotDetail] {
    def writes(rd: RobotDetail): JsObject = Json.obj(
      "state" -> rd.state,
      "rtcSoketUrl" -> rd.rtcSoketUrl
    )
  }
}
