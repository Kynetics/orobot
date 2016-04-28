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

import java.util.{ NoSuchElementException, UUID }

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.source.Indexable
import elastic.EsComponent
import elastic.views.EsView
import org.elasticsearch.action.admin.indices.flush.FlushResponse
import org.elasticsearch.action.delete.DeleteResponse
import org.elasticsearch.action.update.UpdateResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait EsDao[M] {
  self: EsComponent with EsView[M] =>

  private implicit val indexable: Indexable[M] = new Indexable[M] {
    override def json(t: M): String = toJson(t)
  }

  private implicit val hitAs: HitAs[M] = new HitAs[M] {
    override def as(hit: RichSearchHit): M = fromJson(hit.sourceAsString)
  }

  def doFlush(): Future[FlushResponse] =
    client execute (flush index indexName)

  def insert(m: M): Future[IndexResult] =
    client execute (index into indexName / mappingName source m id idOf(m))

  def insert(ms: Iterable[M]): Future[BulkResult] =
    client execute (bulk(ms.map(m => index into indexName / mappingName source m id idOf(m))))

  def update(m: M): Future[UpdateResponse] =
    client execute (ElasticDsl.update id idOf(m) in indexName / mappingName source m)

  def getRaw(id: UUID): Future[RichGetResponse] =
    client execute (ElasticDsl.get id id from indexName / mappingName)

  def get(id: UUID): Future[Option[M]] =
    getRaw(id)
      .filter(_.isExists)
      .map(r => Some(fromJson(r.sourceAsString)))
      .recover {
        case e: NoSuchElementException => None
      }

  def delete(id: UUID): Future[DeleteResponse] =
    client execute (ElasticDsl.delete id id from indexName / mappingName)

  def allRaw(pageReq: PageRequest = PageRequest(1, 20))(implicit manifest: Manifest[M]): Future[RichSearchResponse] = {
    val offset = (pageReq.pageNumber - 1) * pageReq.pageSize
    client execute (search in indexName / mappingName start offset limit pageReq.pageSize)
  }

  def all(pageReq: PageRequest = PageRequest(1, 20))(implicit manifest: Manifest[M]): Future[PagedResult[M]] =
    allRaw(pageReq).map(r => PagedResult(r.totalHits, pageReq, r.as[M]))

}

case class PageRequest(pageNumber: Int, pageSize: Int) {
  require(pageNumber >= 1)
  require(1 < pageSize && pageSize <= 1000)
}

case class PagedResult[T](total: Long, page: PageRequest, items: Iterable[T])
