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

import mqtt.protocol.MqttMessage
import play.api.libs.json.Json
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType.{ DoubleType, IntegerType, LongType, StringType }
import com.sksamuel.elastic4s.mappings.TypedFieldDefinition
import elastic.json.MqttMessageEventJson
import model.MqttMessageEvent

/**
 * Model object used to map mqtt messages index on ElasticSearch
 */
trait MqttMessageView extends EsView[MqttMessageEvent] with MqttMessageEventJson {

  override def idOf(m: MqttMessageEvent): UUID = m.payload.get.\("messageUuid").as[UUID]

  override def toJson(m: MqttMessageEvent): String = Json.stringify(Json.toJson(m))

  override def fromJson(json: String): MqttMessageEvent = Json.fromJson[MqttMessageEvent](Json.parse(json)).get

  override val mappingName: String = "events"

  override val indexName: String = s"orobots_$mappingName"

  override val fieldDefinitions: Seq[TypedFieldDefinition] = Seq(
    field("header") typed StringType index "not_analyzed",
    field("payload") inner (
      field("priority") typed StringType index "not_analyzed",
      field("messageUuid") typed StringType index "not_analyzed",
      field("robotId") typed StringType index "not_analyzed",
      field("x") typed DoubleType index "not_analyzed",
      field("y") typed DoubleType index "not_analyzed",
      field("timestamp") typed LongType index "not_analyzed",
      field("personId") typed IntegerType index "not_analyzed",
      field("nome") typed StringType index "not_analyzed"
    ),
    field("timestampRec") typed LongType index "not_analyzed"
  )
}