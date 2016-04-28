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
import java.io._
import java.nio.file.{Path, _}
import java.nio.file.attribute.BasicFileAttributes
import java.time.{Clock, Instant}
import java.util.{Base64, UUID}
import java.util.concurrent.{CountDownLatch, TimeUnit, TimeoutException}
import java.util.zip.GZIPOutputStream

import akka.actor.ActorSystem
import akka.pattern._
import akka.util.Timeout
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import elastic.DefaultEsComponent
import elastic.json.{MqttMessageEventJson, RobotEventJson}
import elastic.views.EsView
import env.DefaultEnvironment
import model.{Address, Customer, Robot, RobotSummary}
import org.scalatest.{BeforeAndAfterAll, FeatureSpec, GivenWhenThen}
import play.api.libs.json.Json
import protocol.RobotActor.Create
import protocol.RobotsSupervisor.Cmd4Robot

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.sys.process.{Process, ProcessLogger}
import scala.util.{Failure, Random, Success, Try}

class ORobotsSpecs
    extends FeatureSpec
      with BeforeAndAfterAll
      with GivenWhenThen
      with MqttMessageEventJson
      with RobotEventJson {
  import ORobotsSpecs._

  var mosquitto:Process = _
  var system:ActorSystem = _

  override def beforeAll(): Unit = {

    println("starting mosquitto...")
    startMosquitto match {
      case Failure(e) =>
        println(s"unable to start mosquitto due to: $e")
        fail(e)

      case Success(mosquittoProc) =>
        println("mosquitto succesfully started.")
        mosquitto = mosquittoProc
    }

    println("startng akka system...")
    system = startAkkaSystem
    val indexCreationFuture = esComponent.createIndexAndMapping(robotDao.asInstanceOf[EsView[Robot]])(system.dispatcher)
    Await.ready (indexCreationFuture, 3.seconds)

    Files.createDirectory(Paths.get(mapsDirStr))
  }

  override def afterAll(): Unit = {
    println("stopping akka system...")
    Await.result(system.terminate(), 5.seconds)

    println("stopping mosquitto...")
    mosquitto.destroy()
    println(s"mosquitto stopped with exitCode: ${mosquitto.exitValue()}")

    println("remove es data...")
    deleteTestDirs(esDataDirStr)

    println("remove maps data...")
    deleteTestDirs(mapsDirStr)
  }

  info(
    """As an Operator
      |ecc...
    """.stripMargin)

  feature ("Adding a New Robot") {
    scenario("An operator adds a new robot via web interface") {

      Given("a customer")
      val customer = Customer(
        "TSTTST70A01G224G ",
        "test",
        "test",
        "test.test@domain.com",
        Address(
          "via Roma 15/1",
          "35100",
          "Padova",
          "PD"
        ),
        "3481234567"
      )

      When("an operator send a create robot with customer command to the system")
      implicit val ec = system.dispatcher
      implicit val timeout = Timeout(5.seconds)
      val facade = system.actorSelection("/user/orobot/facade")
      val robotId = UUID.randomUUID()
      val result = (facade ? Cmd4Robot(robotId,Create(customer))).map {
        case protocol.RobotActor.Initialized(robot) =>
          Then("the system respond with an Initialized response with the created robot")
          assertResult(customer)(robot.customer)
          Thread.sleep(5000) //wait for elasticsearch flushes writes
          assertResult(Some(robot))(Await.result(robotDao.get(robot.id), 250.millis))
          robot

        case m =>
          assert(false)
          m
      }
      Await.result(result, 6.seconds)

      When("robot is created")
      val resultList = (facade ? protocol.RobotRegistrySupervisor.Cmd4Registry(protocol.RobotRegistryActor.RobotsList(1,20,None))).map {
        case protocol.RobotRegistryActor.RobotListResult(total, items) =>
          Then("it would be in result of robots list")
          assert(total == 1)
          assert(items(0)._1 == robotId)
        case _ =>
          assert(false)
      }
      Await.result(resultList, 6.seconds)

      When("robot is configured and it receives a Fallen Person event")
      val robot = system.actorSelection(s"/user/orobot/robots/$robotId")
      robot ! mqtt.protocol.Configure(robotId, imageToTxt("orobot-akka/src/e2e/scala/map.png"))
      Thread.sleep(5000)
      val messageId = UUID.randomUUID()
      val res = robot ! mqtt.protocol.FallenPerson(Some(mqtt.protocol.PriorityHigh), messageId, robotId, 1.0, 1.0, Instant.now(Clock.systemUTC()).toEpochMilli)
      Thread.sleep(5000)
      val eventList = (facade ? protocol.RobotRegistrySupervisor.Cmd4Registry(protocol.RobotRegistryActor.EventsList(1,20))).map {
        case protocol.RobotRegistryActor.EventListResult(res) =>
          Then("it would be in result of event list")
          assert(res.total == 1)
          println(s"Events=${Json.toJson(res.items.toSeq)}")
        case _ =>
          assert(false)
      }
      Await.result(eventList, 6.seconds)

      When("event detail is required")
      val robotEventList = (facade ? protocol.RobotRegistrySupervisor.Cmd4Registry(protocol.RobotRegistryActor.EventDetail(messageId))).map {
        case protocol.RobotRegistryActor.EventDetailResult(res) =>
          Then("it would be shown")
          assert(res.get.payload.get.\("messageUuid").as[UUID] == messageId)
          println(s"Event=${Json.toJson(res)}")
        case _ =>
          assert(false)
      }
      Await.result(robotEventList, 6.seconds)

    }
  }

}

object ORobotsSpecs extends DefaultEnvironment {

  def imageToTxt(filename: String) = {
    val file = new File(filename)
    val in = new FileInputStream(file)
    val bytes = new Array[Byte](file.length.toInt)
    in.read(bytes)
    in.close()

    val arrOutputStream = new ByteArrayOutputStream()
    val zipOutputStream = new GZIPOutputStream(arrOutputStream)
    zipOutputStream.write(bytes)
    zipOutputStream.close()
    Base64.getEncoder.encodeToString(arrOutputStream.toByteArray)
  }

  val esDataDirStr = s"es-test-${new String(Random.alphanumeric.take(8).toArray)}"
  val mapsDirStr = s"maps-${new String(Random.alphanumeric.take(8).toArray)}"

  val mqttPort = 1885

  override def cfg = ConfigFactory.empty().withValue(
    "elastic.local.path.home",
    ConfigValueFactory.fromAnyRef(esDataDirStr)
    ).withValue(
      "elastic.local.path.data",
      ConfigValueFactory.fromAnyRef(esDataDirStr+"/data")
    )
    .withValue(
      "akka.persistence.journal.plugin",
      ConfigValueFactory.fromAnyRef("inmemory-journal"))
    .withValue(
      "akka.persistence.snapshot-store.plugin",
      ConfigValueFactory.fromAnyRef("inmemory-snapshot-store"))
    .withValue(
      "mosquitto.broker.url",
      ConfigValueFactory.fromAnyRef(s"tcp://localhost:$mqttPort"))
    .withValue("robot.locationMap.folder", ConfigValueFactory.fromAnyRef(mapsDirStr))

  @transient override lazy val esComponent = new DefaultEsComponent(Some(cfg))


  private def startMosquitto: Try[Process] = Try {
    val cl = new CountDownLatch(1)
    val pl = ProcessLogger { line =>
      println(line)
      if (line.contains(s"Opening ipv6 listen socket on port $mqttPort."))
        cl.countDown()
    }
    import scala.sys.process._
    val path = System.getenv("PATH")
    val whichMosquitto = Process(Seq("which", "mosquitto"), None, "PATH" -> s"$path:/usr/local/sbin").lineStream_!
    val proc = s"${whichMosquitto.mkString} -p $mqttPort".run(pl)
    if (cl.await(3L, TimeUnit.SECONDS))
      proc
    else
      throw new TimeoutException()
  }

  private def startAkkaSystem: ActorSystem = {
    val system = ActorSystem("ORobotsSpecs", cfg)
    actors.initSystem(system, this)
    system
  }

  def deleteTestDirs(toDelete: String) = Files.walkFileTree(Paths.get(toDelete), new SimpleFileVisitor[Path]{
    def delete(path:Path) = {Files.delete(path); FileVisitResult.CONTINUE}
    override def visitFile(file:Path, attrs:BasicFileAttributes):FileVisitResult = delete(file)
    override def postVisitDirectory(dir:Path, ioe:IOException):FileVisitResult = delete(dir)
  })

}