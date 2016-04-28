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

trait CustomerJson extends AddressJson {

  implicit val customerReads: Reads[model.Customer] = (
    (JsPath \ "codiceFiscale").read(minLength[String](1) /*orError("Required")*/ ) and
    (JsPath \ "firstName").read(minLength[String](1)) and
    (JsPath \ "lastName").read(minLength[String](1)) and
    (JsPath \ "email").read(minLength[String](1)) and
    (JsPath \ "address").read[model.Address] and
    (JsPath \ "phone").read(minLength[String](1))
  )(model.Customer.apply _)

  implicit val customerWrites: Writes[model.Customer] = new Writes[model.Customer] {
    def writes(customer: model.Customer): JsObject = Json.obj(
      "codiceFiscale" -> customer.codiceFiscale,
      "firstName" -> customer.firstName,
      "lastName" -> customer.lastName,
      "email" -> customer.email,
      "address" -> customer.address,
      "phone" -> customer.phone
    )
  }

}
