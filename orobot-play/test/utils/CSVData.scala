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

import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader

import play.api.Play
import play.api.Play.current

class CSVData(fileName: String,
    charset: String = "UTF-8", separator: Char = ',', quote: Char = '"', escape: Char = '\\', skipFirst: Boolean = false) extends Traversable[Array[String]] {

  override def foreach[U](f: Array[String] => U): Unit = {

    val rs = Play.resourceAsStream(fileName)

    val reader = new BufferedReader(new InputStreamReader(rs.get, charset))
    try {
      if (skipFirst) reader.readLine()
      var next = true
      while (next) {
        val line = reader.readLine()
        if (line != null && line.trim.nonEmpty) f(parse(line))
        else next = false
      }
    } finally {
      reader.close()
    }
  }

  private def parse(line: String): Array[String] = {
    val values = Array.newBuilder[String]
    val buffer = new StringBuilder
    var insideQuotes = false
    var escapeNext = false
    for (c <- line) {
      if (escapeNext) { buffer += c; escapeNext = false }
      else if (c == escape) escapeNext = true
      else if (c == quote) insideQuotes = !insideQuotes
      else if (c == separator && !insideQuotes) { values += buffer.result; buffer.clear }
      else buffer += c
    }
    values += buffer.result
    return values.result
  }

}