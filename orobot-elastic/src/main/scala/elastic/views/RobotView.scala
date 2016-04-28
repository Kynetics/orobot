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
import com.sksamuel.elastic4s.mappings.FieldType.{ IntegerType, StringType }
import com.sksamuel.elastic4s.mappings.TypedFieldDefinition
import elastic.json.RobotJson
import model.Robot
import play.api.libs.json.Json

/**
 * Model object used to map Robots index on ElasticSearch
 */
trait RobotView extends EsView[Robot] with RobotJson {

  override def idOf(r: Robot): UUID = r.id

  override def toJson(r: Robot): String = Json.stringify(Json.toJson(r))

  override def fromJson(json: String): Robot = Json.fromJson[Robot](Json.parse(json)).get

  override val mappingName: String = "robots"

  override val indexName: String = s"orobots_$mappingName"

  override val fieldDefinitions: Seq[TypedFieldDefinition] = Seq(

    field("id") typed StringType index "not_analyzed",

    field("customer") inner (
      field("codiceFiscale") typed StringType index "not_analyzed",
      field("firstName") typed StringType index "not_analyzed",
      field("lastName") typed StringType index "not_analyzed",
      field("email") typed StringType index "not_analyzed",
      field("phone") typed IntegerType index "not_analyzed",

      field("address") inner (
        field("street") typed StringType index "not_analyzed",
        field("cap") typed IntegerType index "not_analyzed",
        field("city") typed StringType index "not_analyzed",
        field("pv") typed StringType index "not_analyzed"
      )
    )
  )
}