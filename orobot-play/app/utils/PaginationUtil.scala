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
package utils

import java.net.URI

class PaginationUtil(page: Int, size: Int, totalItems: Long, baseUrl: String) {

  val paginationHttpHeaders = {
    val offset = (page - 1) * size
    val totalPages = (totalItems / size) + (if (totalItems % size > 0) 1 else 0)
    var link: String = ""
    if (offset < totalPages) {
      link = "<" + new URI(baseUrl + "?page=" + (offset + 1) + "&size=" + size).toString + ">; rel=\"next\","
    }
    if (offset > 1) {
      link += "<" + new URI(baseUrl + "?page=" + (offset - 1) + "&size=" + size).toString + ">; rel=\"prev\","
    }
    link += "<" + new URI(baseUrl + "?page=" + totalPages + "&size=" + size).toString + ">; rel=\"last\"," +
      "<" + new URI(baseUrl + "?page=" + 1 + "&size=" + size).toString + ">; rel=\"first\""
    link
  }
}
