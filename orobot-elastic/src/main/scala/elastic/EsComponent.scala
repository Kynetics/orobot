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
package elastic

import java.io.File

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ ElasticClient, ElasticsearchClientUri }
import com.typesafe.config.ConfigException.BadValue
import com.typesafe.config.{ Config, ConfigFactory }
import elastic.views.EsView
import net.ceedubs.ficus.Ficus._
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse
import org.elasticsearch.common.settings.Settings

import scala.collection.convert.WrapAsScala._
import scala.concurrent.{ ExecutionContext, Future }

trait EsComponent {

  def client: ElasticClient

  def createIndexAndMapping(view: EsView[_])(implicit ec: ExecutionContext): Future[CreateIndexResponse] = {
    val indexName = view.indexName
    val chkIndexStmt = index exists indexName
    val crtIndexStmt = create index indexName mappings (mapping(view.mappingName) fields view.fieldDefinitions)
    (client execute chkIndexStmt)
      .filter(_.isExists)
      .flatMap(r => client execute crtIndexStmt)
  }
}

class DefaultEsComponent(config: Option[Config] = None) extends EsComponent {

  private def getAbsolutePath(key: String, cf: Config): Option[String] =
    cf.getAs[String](key).map { s =>
      val f = new File(s)
      if (f.isAbsolute) s
      else f.getAbsolutePath
    }

  override val client = {
    val defConf = ConfigFactory.load()
    val conf = config.map(_.withFallback(defConf)).getOrElse(defConf)
    val mode = conf.getAs[String]("elastic.mode").getOrElse("local")
    if ("local" == mode) {
      val locConf = conf.getConfig("elastic.local")
      val pathHome = getAbsolutePath("path.home", locConf).getOrElse("es-test")
      val pathData = getAbsolutePath("path.data", locConf).getOrElse("es-test/data")
      val sb = Settings.builder()
        .put("path.home", pathHome)
        .put("path.data", pathData)
      locConf.entrySet()
        .filter(e => e.getKey != "path.home" && e.getKey != "path.data")
        .foreach { e =>
          sb.put(e.getKey, e.getValue.unwrapped())
        }
      ElasticClient.local(sb.build())
    } else if ("remote" == mode) {
      val remConf = conf.getConfig("elastic.remote")
      val addrs = remConf.getAs[String]("addresses").getOrElse("elasticsearch://localhost:9300")
      val sb = Settings.builder()
      remConf.entrySet()
        .filter(e => e.getKey != "addresses")
        .foreach { e =>
          sb.put(e.getKey, e.getValue.unwrapped())
        }
      ElasticClient.transport(sb.build(), ElasticsearchClientUri(addrs))
    } else {
      throw new BadValue("elastic.mode", "allowed values are local or remote")
    }
  }
}