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
package elastic.daos

import com.sksamuel.elastic4s.ElasticDsl.{ field, _ }
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.source.Indexable
import elastic.EsComponent
import elastic.views.EsView
import org.elasticsearch.search.sort.SortOrder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait RobotEventDao[M] extends EsDao[M] {
  self: EsComponent with EsView[M] =>

  private implicit val indexable: Indexable[M] = new Indexable[M] {
    override def json(t: M): String = toJson(t)
  }

  private implicit val hitAs: HitAs[M] = new HitAs[M] {
    override def as(hit: RichSearchHit): M = fromJson(hit.sourceAsString)
  }

  override def allRaw(pageReq: PageRequest = PageRequest(1, 20))(implicit manifest: Manifest[M]): Future[RichSearchResponse] = {
    val offset = (pageReq.pageNumber - 1) * pageReq.pageSize
    client execute (
      search in indexName / mappingName
      sourceInclude ("header", "payload.priority", "payload.messageUuid", "payload.robotId", "payload.timestamp", "timestampRec")
      sort (
        field sort "payload.priority" order SortOrder.ASC,
        field sort "payload.timestamp" order SortOrder.DESC
      )
        start offset limit pageReq.pageSize
    )
  }

  override def all(pageReq: PageRequest = PageRequest(1, 20))(implicit manifest: Manifest[M]): Future[PagedResult[M]] =
    allRaw(pageReq).map(r => PagedResult(r.totalHits, pageReq, r.as[M]))

  def allByRobotId(robotId: String, pageReq: PageRequest = PageRequest(1, 20))(implicit manifest: Manifest[M]): Future[PagedResult[M]] = {
    val offset = (pageReq.pageNumber - 1) * pageReq.pageSize
    val res = client execute (
      search in indexName / mappingName
      sourceInclude ("header", "payload.priority", "payload.messageUuid", "payload.robotId", "payload.timestamp", "timestampRec")
      query bool {
        must(
          matchPhraseQuery("payload.robotId", robotId)
        )
      }
      sort (
        field sort "payload.priority" order SortOrder.ASC,
        field sort "payload.timestamp" order SortOrder.DESC
      )
        start offset limit pageReq.pageSize
    )
    res.map(r => PagedResult(r.totalHits, pageReq, r.as[M]))
  }
}

