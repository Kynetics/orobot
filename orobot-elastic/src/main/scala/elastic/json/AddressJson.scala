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
package elastic.json

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

trait AddressJson {

  implicit val addressReads: Reads[model.Address] = (
    (JsPath \ "street").read(minLength[String](1)) and
    (JsPath \ "cap").read(minLength[String](1)) and
    (JsPath \ "city").read(minLength[String](1)) and
    (JsPath \ "pv").read(minLength[String](1))
  )(model.Address.apply _)

  implicit val addressWrites: Writes[model.Address] = new Writes[model.Address] {
    def writes(address: model.Address): JsObject = Json.obj(
      "street" -> address.street,
      "cap" -> address.cap,
      "city" -> address.city,
      "pv" -> address.pv
    )
  }

}
