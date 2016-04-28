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

import play.api._
import play.api.mvc._
import no.samordnaopptak.apidoc.ApiDoc
import no.samordnaopptak.apidoc.ApiDocUtil
import no.samordnaopptak.json.J

class ApiDocController extends Controller {

  // The required swagger info object (see https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md#infoObject)
  val swaggerInfoObject = J.obj(
    "info" -> J.obj(
      "title" -> "Generated Swagger API",
      "version" -> "1.0"
    )
  )

  @ApiDoc(doc = """
    GET /api/v1/api-docs

    DESCRIPTION
      Get main swagger json documentation
              """)
  def getJson() = Action {
    val generatedSwaggerDocs = ApiDocUtil.getSwaggerDocs()
    val json = generatedSwaggerDocs ++ swaggerInfoObject
    Ok(json.asJsValue)
  }

  def getUi = Action {
    import play.api.Play
    val jsonApiPath = routes.ApiDocController.getJson().path()
    val swaggerUiCall = routes.Assets.at("swagger.html")
    Redirect(swaggerUiCall.path(), Map("url" -> Seq(jsonApiPath)))
  }

}