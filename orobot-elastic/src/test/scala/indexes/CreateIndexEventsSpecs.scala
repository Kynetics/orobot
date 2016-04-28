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
package indexes

import java.io.IOException
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.time.{ Clock, Instant }
import java.util.UUID

import com.sksamuel.elastic4s.ElasticDsl.{ field, _ }
import com.typesafe.config.{ ConfigFactory, ConfigValueFactory }
import elastic.daos.{ EsDao, RobotEventDao }
import elastic.json.{ MqttMessageEventJson, RobotEventJson }
import elastic.views.{ MqttMessageView, RobotEventView }
import elastic.{ DefaultEsComponent, EsComponent }
import model.{ MqttMessageEvent, RobotEventSummary }
import mqtt.protocol._
import org.elasticsearch.search.sort.SortOrder
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }
import play.api.libs.json.Json

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.Random

class CreateIndexEventsSpecs extends WordSpec with Matchers with BeforeAndAfterAll with MqttMessageEventJson with RobotEventJson {

  import CreateIndexEventsSpecs._

  override def afterAll(): Unit = {
    println("remove es data...")
    deleteESDir
  }

  "CreateFallenPersonEvent" should {
    "index fallen_person event" in {

      val msgUuid = UUID.randomUUID()
      val rUuid = UUID.randomUUID()
      val tsRobot = Instant.now(Clock.systemUTC()).toEpochMilli
      val fp = FallenPerson(Some(PriorityHigh), msgUuid, rUuid, 1.0, 1.0, tsRobot)
      Thread.sleep(1000)
      val tsRec = Instant.now(Clock.systemUTC()).toEpochMilli
      val idx = Await.result(mqttMessageDao.insert(MqttMessageEvent(fp.header, fp.toJson, tsRec)), 2.seconds)
      println(s"Index=$idx")
      idx.index shouldBe "orobots_events"
      idx.created shouldBe true

      Thread.sleep(3000)

      val resp = esComponent.client.execute { search in "orobots_events" / "events" sourceInclude ("header", "payload.priority", "payload.messageUuid", "payload.robotId", "payload.timestamp", "timestampRec") }.await
      println(s"resp=$resp")

      val msgs = Await.result(robotEventDao.all(), 2.seconds)
      println(s"msgs=$msgs")
      msgs.total shouldBe 1

      val byRobot = Await.result(robotEventDao.allByRobotId(rUuid.toString), 2.seconds)
      println(s"byRobot=$byRobot")
      byRobot.total shouldBe 1
    }
  }

  "CreateManyEvents" should {
    "index some events and search" in {

      val rUuid1 = UUID.randomUUID()

      val fp1 = FallenPerson(Some(PriorityHigh), UUID.randomUUID(), rUuid1, 1.0, 1.0, Instant.now(Clock.systemUTC()).toEpochMilli)
      val fp2 = FallenPerson(Some(PriorityHigh), UUID.randomUUID(), UUID.randomUUID(), 1.0, 1.0, Instant.now(Clock.systemUTC()).toEpochMilli)

      val dp1 = DetectedPerson(Some(PriorityNormal), UUID.randomUUID(), rUuid1, 1.0, 1.0, 4, Instant.now(Clock.systemUTC()).toEpochMilli)
      val dp2 = DetectedPerson(Some(PriorityNormal), UUID.randomUUID(), UUID.randomUUID(), 1.0, 1.0, 5, Instant.now(Clock.systemUTC()).toEpochMilli)
      val dp3 = DetectedPerson(Some(PriorityLow), UUID.randomUUID(), UUID.randomUUID(), 1.0, 1.0, 6, Instant.now(Clock.systemUTC()).toEpochMilli)

      val rp1 = RecognizedPerson(Some(PriorityNormal), UUID.randomUUID(), rUuid1, "Test001", 4, Instant.now(Clock.systemUTC()).toEpochMilli)
      val rp2 = RecognizedPerson(Some(PriorityNormal), UUID.randomUUID(), UUID.randomUUID(), "Test002", 5, Instant.now(Clock.systemUTC()).toEpochMilli)
      val rp3 = RecognizedPerson(Some(PriorityLow), UUID.randomUUID(), UUID.randomUUID(), "Test003", 6, Instant.now(Clock.systemUTC()).toEpochMilli)

      val bl1 = BatteryLevel(Some(PriorityHigh), UUID.randomUUID(), rUuid1, Instant.now(Clock.systemUTC()).toEpochMilli)
      val bl2 = BatteryLevel(Some(PriorityHigh), UUID.randomUUID(), UUID.randomUUID(), Instant.now(Clock.systemUTC()).toEpochMilli)

      Await.result(mqttMessageDao.insert(MqttMessageEvent(fp1.header, fp1.toJson, Instant.now(Clock.systemUTC()).toEpochMilli)), 2.seconds)
      Await.result(mqttMessageDao.insert(MqttMessageEvent(fp2.header, fp2.toJson, Instant.now(Clock.systemUTC()).toEpochMilli)), 2.seconds)
      Await.result(mqttMessageDao.insert(MqttMessageEvent(dp1.header, dp1.toJson, Instant.now(Clock.systemUTC()).toEpochMilli)), 2.seconds)
      Await.result(mqttMessageDao.insert(MqttMessageEvent(dp2.header, dp2.toJson, Instant.now(Clock.systemUTC()).toEpochMilli)), 2.seconds)
      Await.result(mqttMessageDao.insert(MqttMessageEvent(dp3.header, dp3.toJson, Instant.now(Clock.systemUTC()).toEpochMilli)), 2.seconds)
      Await.result(mqttMessageDao.insert(MqttMessageEvent(rp1.header, rp1.toJson, Instant.now(Clock.systemUTC()).toEpochMilli)), 2.seconds)
      Await.result(mqttMessageDao.insert(MqttMessageEvent(rp2.header, rp2.toJson, Instant.now(Clock.systemUTC()).toEpochMilli)), 2.seconds)
      Await.result(mqttMessageDao.insert(MqttMessageEvent(rp3.header, rp3.toJson, Instant.now(Clock.systemUTC()).toEpochMilli)), 2.seconds)
      Await.result(mqttMessageDao.insert(MqttMessageEvent(bl1.header, bl1.toJson, Instant.now(Clock.systemUTC()).toEpochMilli)), 2.seconds)
      Await.result(mqttMessageDao.insert(MqttMessageEvent(bl2.header, bl2.toJson, Instant.now(Clock.systemUTC()).toEpochMilli)), 2.seconds)

      Thread.sleep(3000)

      val resp = esComponent.client.execute {
        search in "orobots_events" / "events" sort (
          field sort "payload.priority" order SortOrder.ASC,
          field sort "payload.timestamp" order SortOrder.DESC
        )
      }.await
      println(s"es search=$resp")

      val msgs = Await.result(robotEventDao.all(), 2.seconds)
      println(s"msgs=${msgs}")
      msgs.total shouldBe 11

      val byRobot = Await.result(robotEventDao.allByRobotId(rUuid1.toString), 2.seconds)
      println(s"byRobot=${byRobot}")
      byRobot.total shouldBe 4

      import scala.concurrent.ExecutionContext.Implicits.global
      val f = robotEventDao.all()
      f.map { r =>
        println(s"events${Json.toJson(r.items.toSeq)}")
      }
    }
  }
}

object CreateIndexEventsSpecs {

  val esDataDirStr = s"es-test-${new String(Random.alphanumeric.take(8).toArray)}"

  def cfg = ConfigFactory.empty().withValue(
    "elastic.local.path.home",
    ConfigValueFactory.fromAnyRef(esDataDirStr)
  ).withValue(
      "elastic.local.path.data",
      ConfigValueFactory.fromAnyRef(esDataDirStr + "/data")
    )

  @transient lazy val esComponent = new DefaultEsComponent(Some(cfg))

  val mqttMessageDao: EsDao[MqttMessageEvent] =
    new EsDao[MqttMessageEvent] with EsComponent with MqttMessageView {
      override val client = esComponent.client
    }

  val robotEventDao: RobotEventDao[RobotEventSummary] =
    new RobotEventDao[RobotEventSummary] with EsComponent with RobotEventView {
      override val client = esComponent.client
    }

  def deleteESDir = Files.walkFileTree(Paths.get(esDataDirStr), new SimpleFileVisitor[Path] {
    def delete(path: Path) = { Files.delete(path); FileVisitResult.CONTINUE }
    override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = delete(file)
    override def postVisitDirectory(dir: Path, ioe: IOException): FileVisitResult = delete(dir)
  })
}
