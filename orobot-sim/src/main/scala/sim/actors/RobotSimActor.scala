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
package sim.actors

import java.time._
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

import akka.actor.{ Actor, ActorLogging, ActorRef, Cancellable, Props, ReceiveTimeout, Terminated }
import akka.event.LoggingReceive
import mqtt.actors.{ MqttConnectorActor, MqttConnectorSupervisor }
import net.ceedubs.ficus.Ficus._
import mqtt.protocol._
import MqttConnectorActor._
import sim.{ SimApp, Simulation }

import scala.concurrent.duration._
import scalafx.application.{ JFXApp, Platform }

object RobotSimActor {

  val name = "robotsim"
  val props = Props(classOf[RobotSimActor])

  case object SIMULATION_TICK

  import scala.language.implicitConversions
  implicit def func2Runnable(f: () => Unit) = new Runnable {
    override def run(): Unit = f()
  }

}

class RobotSimActor
    extends Actor
    with ActorLogging {

  import RobotSimActor._

  import context.dispatcher

  log.debug("actor path:{}", self.path)

  val cfg = context.system.settings.config

  var keepAliveTime: Option[LocalTime] = None
  var simSched: Option[Cancellable] = None
  var keepAliveSched: Option[Cancellable] = None

  val sim = Simulation(cfg.getConfig("sim.env"))() //WARNING MUTABLE!!!

  val homeImageTxt: AtomicReference[String] = new AtomicReference[String]()

  val jfx = new JFXApp with SimApp {
    override def sim = RobotSimActor.this.sim
    stage = myStage
  }

  var watchedByMap: Map[UUID, LocalTime] = Map.empty
  var positionSenderSched: Option[Cancellable] = None

  var fallenPersonSched: Option[Cancellable] = None
  var detectedPersonSched: Option[Cancellable] = None
  var recognizedersonSched: Option[Cancellable] = None
  var batteryLevelSched: Option[Cancellable] = None

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    super.preStart()
    context.dispatcher.execute(() => jfx.main(Array.empty))
    Thread.sleep(2000)

    val countDownLatchImg = new CountDownLatch(1)
    Platform.runLater({
      homeImageTxt.set(sim.home.homeImageAsTxt)
      countDownLatchImg.countDown()
    })
    countDownLatchImg.await()
  }

  val dt = (1000 / 60).millis

  val mqttConnectorSel = context.actorSelection(s"../${MqttConnectorSupervisor.name}")

  val uuidStr = cfg.as[String]("sim.robot.id")

  val uuid = UUID.fromString(uuidStr)

  val needSendConfiguration = cfg.as[Boolean]("sim.robot.registerAtStartup")

  log.info("Registering to mqttConnector")

  mqttConnectorSel ! Register(uuid)

  context.setReceiveTimeout(5.seconds)

  val receive: Receive = Actor.emptyBehavior

  val waitingForRegisteredMqttConfirm: Receive = LoggingReceive {

    case Registered =>

      val mqttConnector = sender()
      context watch mqttConnector
      if (needSendConfiguration) {
        val uuid = UUID.randomUUID()
        mqttConnector ! Configure(uuid, homeImageTxt.get())
        context become waitingForConfigureConfirm(uuid, mqttConnector)
      } else becomeActive(mqttConnector)

    case ReceiveTimeout =>
      log.info("ReceiveTimeout while waitingForRegisteredMqttConfirm")
      context stop self

  }

  def waitingForConfigureConfirm(uuid: UUID, mqttConnector: ActorRef): Receive = LoggingReceive {

    case Ack(Some(`uuid`)) =>
      log.debug(s"json message=${toJson(Ack(Some(`uuid`)))}")
      becomeActive(mqttConnector)

    case ReceiveTimeout =>
      log.info("ReceiveTimeout while waitingForConfigureConfirm")
      context stop self

    case Terminated(`mqttConnector`) =>
      log.info("Mqtt Connector terminated!")
      context stop self
  }

  def active(mqttConnector: ActorRef): Receive = LoggingReceive {

    case SIMULATION_TICK => processTic

    case Alive =>
      log.debug(s"json message=${toJson(Alive)}")
      keepAliveTime = Some(LocalTime.now())

    case ReceiveTimeout =>
      log.info("ReceiveTimeout while active")
      context stop self

    case Terminated(`mqttConnector`) =>
      log.info("Mqtt Connector terminated!")
      context stop self

    case WatchedBy(operatorId) if watchedByMap.contains(operatorId) =>
      log.debug(s"json message=${toJson(WatchedBy(operatorId))}")
      watchedByMap += operatorId -> LocalTime.now()

    case WatchedBy(operatorId) =>
      log.debug(s"json message=${toJson(WatchedBy(operatorId))}")
      watchedByMap += operatorId -> LocalTime.now()
      log.info(s"WatchedBy $operatorId")
      positionSenderSched = Some(context.system.scheduler.schedule(1.seconds, 1.seconds, () => {
        mqttConnector ! Position(uuid, sim.robot.position.x, sim.robot.position.y, sim.robot.heading)
      }))

    case MovedTo(x, y) =>
      log.debug(s"MOVING TO ... json message=${toJson(MovedTo(x, y))}")
  }

  private def becomeActive(mqttConnector: ActorRef): Unit = {
    context.setReceiveTimeout(15.seconds)
    keepAliveSched = Some(context.system.scheduler.schedule(15.seconds, 15.seconds, mqttConnector, KeepAlive))
    simSched = Some(context.system.scheduler.schedule(dt, dt, self, SIMULATION_TICK))
    keepAliveTime = Some(LocalTime.now())
    val currentTimestamp = Instant.now(Clock.systemUTC()).toEpochMilli
    fallenPersonSched = Some(context.system.scheduler.schedule(20.seconds, 20.seconds, mqttConnector, FallenPerson(Some(PriorityHigh), UUID.randomUUID(), uuid, sim.robot.position.x, sim.robot.position.y, currentTimestamp)))
    detectedPersonSched = Some(context.system.scheduler.schedule(25.seconds, 20.seconds, mqttConnector, DetectedPerson(Some(PriorityNormal), UUID.randomUUID(), uuid, sim.robot.position.x, sim.robot.position.y, 1, currentTimestamp)))
    recognizedersonSched = Some(context.system.scheduler.schedule(30.seconds, 20.seconds, mqttConnector, RecognizedPerson(Some(PriorityNormal), UUID.randomUUID(), uuid, "Test Person", 1, currentTimestamp)))
    batteryLevelSched = Some(context.system.scheduler.schedule(35.seconds, 20.seconds, mqttConnector, BatteryLevel(Some(PriorityHigh), UUID.randomUUID(), uuid, currentTimestamp)))
    context become active(mqttConnector)
  }

  context.become(waitingForRegisteredMqttConfirm)

  @scala.throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    Platform.exit()
    simSched.foreach(_.cancel)
    keepAliveSched.foreach(_.cancel)
    super.postStop()
  }

  def processTic: Unit = {

    Platform.runLater(sim.evolve(1.0 / 60))
    val now = LocalTime.now
    keepAliveTime.foreach { kat =>
      val missingAliveFrom = java.time.Duration.between(kat, now).abs().getSeconds
      if (missingAliveFrom > 15 && sim.robot.connected) Platform.runLater(sim.robot.connected = false)
      else if (missingAliveFrom < 15 && !sim.robot.connected) Platform.runLater(sim.robot.connected = true)
    }

    watchedByMap = watchedByMap.filter { wo =>
      java.time.Duration.between(wo._2, now).abs().getSeconds < 15
    }
    if (watchedByMap.isEmpty) positionSenderSched.foreach(_.cancel())
  }

}