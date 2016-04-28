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

import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * An util class, represent a bad response, not good
 *
 * @param code of error
 * @param error an object that expone the errors
 */
class Bad(val code: Option[Int], val error: JsValue) {
  def status = "ko"
}

/**
 * Companion object for Good class
 */
object Bad {

  def apply(code: Option[Int] = None, message: String) = new Bad(code, JsString(message))
  def apply(code: Option[Int], message: JsValue) = new Bad(code, message)
  def apply(message: JsValue) = new Bad(None, message)
  def unapply(bad: Bad) = Some((bad.status, bad.code, bad.error))

  /**
   * Rest format
   */
  implicit val restFormat: Format[Bad] = {
    val reader: Reads[Bad] = (
      (__ \ "code").readNullable[Int] ~
      (__ \ "error").read[JsValue])(Bad.apply(_, _))

    val writer: Writes[Bad] = (
      (__ \ "status").write[String] ~
      (__ \ "code").writeNullable[Int] ~
      (__ \ "error").write[JsValue])(unlift(Bad.unapply _))

    Format(reader, writer)
  }

}