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
package elastic.views

import java.util.UUID

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType.{ LongType, StringType }
import com.sksamuel.elastic4s.mappings.TypedFieldDefinition
import elastic.json.RobotEventJson
import model.RobotEventSummary
import play.api.libs.json.Json

trait RobotEventView extends EsView[RobotEventSummary] with RobotEventJson {

  override def idOf(m: RobotEventSummary): UUID = m.payload.\("messageId").as[UUID]

  override def toJson(m: RobotEventSummary): String = Json.stringify(Json.toJson(m))

  override def fromJson(json: String): RobotEventSummary = Json.fromJson[RobotEventSummary](Json.parse(json)).get

  override val mappingName: String = "events"

  override val indexName: String = s"orobots_$mappingName"

  //Index not needed. It's search only
  override val fieldDefinitions: Seq[TypedFieldDefinition] = Seq.empty
}