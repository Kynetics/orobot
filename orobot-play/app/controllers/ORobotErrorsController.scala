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
package controllers

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import json.Bad
import models.User
import play.api.libs.json.{ JsError, JsResult, Json }
import play.api.mvc.Result

import scala.concurrent.Future

trait ORobotErrorsController {
  self: Silhouette[User, JWTAuthenticator] =>

  val X_TOTAL_COUNT: String = "X-Total-Count"

  def execute[T](json: JsResult[T])(f: (T) => Future[Result]): Future[Result] = {
    json.map { data: T =>
      f(data)
    } recoverTotal {
      case error =>
        logger.error(s"Error during controller ${error.toString}")
        Future.successful(BadRequest(Json.toJson(Bad(message = JsError.toJson(error)))))
    }
  }

  /*
  Example of the format returned when an error occur:

  {"status":"ko",
   "error": {
     "obj.lastName":[
       {"msg":["error.path.missing"],
        "args":[]}],
     "obj.firstName":[
       {"msg":["error.path.missing"],
        "args":[]}
     ]
     }
   */

  //  def renderValidationErrors(validation: ValidationErrors): Result = {
  //    def msgDetails(errorMessage: ErrorMessage) = {
  //      val msg: String = messagesApi.translate(errorMessage.messageCode, errorMessage.messageParams).getOrElse(errorMessage.messageCode)
  //      arr(obj(
  //        "code" -> List(errorMessage.messageCode),
  //        "msg" -> List(msg),
  //        "args" -> errorMessage.messageParams.map { a => toJson(a.toString) }))
  //    }
  //    val errorsJs: List[(String, JsValue)] = validation.errors.map { e =>
  //      e match {
  //        case f: FieldError => ("obj." + f.field, msgDetails(f))
  //        case g: GenericError => ("generic-error", msgDetails(g))
  //      }
  //    }
  //    Status(validation.code.code)(toJson(Bad(Some(validation.code.code), toJson(errorsJs.toMap[String, JsValue]))))
  //  }

}
