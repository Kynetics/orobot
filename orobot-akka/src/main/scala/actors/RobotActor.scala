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
package actors

import java.io.File
import java.time.{ Clock, Instant }
import java.util.UUID
import javax.imageio.ImageIO

import actors.RobotActor.Env
import actors.RobotRegistryActor.{ DeregisterRobot, RegisterRobot }
import akka.actor._
import akka.event.LoggingReceive
import akka.persistence.{ PersistentActor, RecoveryCompleted, SnapshotOffer }
import elastic.daos._
import model._
import mqtt.actors.MqttConnectorActor.{ Register, Registered }
import mqtt.protocol._
import net.ceedubs.ficus.Ficus._
import persistence.RobotActorPersistence
import protocol.RobotActor._

import scala.concurrent.duration._

object RobotActor {

  trait Env {
    def mqttConnectorSupervisorPath: String
    def robotRegistrySupervisorPath: String
    def robotDao: EsDao[Robot]
    def mqttMessageDao: EsDao[MqttMessageEvent]
    def robotEventDao: RobotEventDao[RobotEventSummary]
  }

  def name(id: UUID) = s"${id.toString}"

  def props(env: Env) = Props(classOf[RobotActor], env)

  //RECEIVED
  case object RegistrationConfirmed extends Received
  case object DeregistrationConfirmed extends Received

  //SENT
  case class WrongRobotState(msg: String) extends Sent //TODO MOVE IN PROTOCOL PACKAGE

}

class RobotActor(val env: Env)
    extends PersistentActor
    with ActorLogging
    with RobotActorPersistence
    with Stash {

  import RobotActor._
  import persistence.RobotActorPersistence.RobotActorEvents._
  import persistence.RobotActorPersistence.State

  log.debug("actor path:{}", self.path)

  lazy val uuidStr = self.path.name

  log.debug(s"RobotActor uuid=$uuidStr")

  lazy val persistenceId: String = classOf[RobotActor].getSimpleName + "_" + uuidStr

  log.debug(s"RobotActor persistenceId=$persistenceId")

  lazy val keepAliveTimeout = context.system.settings.config.getAs[FiniteDuration]("robot.keepalive.timeout").getOrElse(15.minutes)

  lazy val locationMapFolder = context.system.settings.config.getAs[String]("robot.locationMap.folder").getOrElse(System.getProperty("user.dir"))

  val mqttConnectorSupervisorActorSelection = context.actorSelection(env.mqttConnectorSupervisorPath)

  val robotRegistrySupervisor = alodProxy()

  override val receiveCommand = Actor.emptyBehavior

  val oSystemCommands: Receive = LoggingReceive {

    case protocol.RobotActor.Get =>
      log.debug(s"command Get received for robot $uuidStr")
      sender() ! state
  }

  def MyReceive(receive: Receive): Receive = oSystemCommands orElse LoggingReceive(receive)

  override var state = State(UUID.fromString(uuidStr))

  var mqttConnectorActorOpt: Option[ActorRef] = None

  log.debug("mqtt connector supervisor send identify!!!")
  tryMqttConnection(0.seconds)

  var mapWidth: Int = 0
  var mapHeight: Int = 0

  val waitingForMqttActorIdentity: Receive = MyReceive {

    case ActorIdentity(_, Some(actorRef)) =>
      log.info("mqttConnector is defined!!! send Register to mqttConnector")
      actorRef ! Register(UUID.fromString(uuidStr))
      context.setReceiveTimeout(3.seconds)
      context become waitingForMqttConnection

    case ActorIdentity(_, None) =>
      log.info("mqttConnector is not defined. Retry")
      tryMqttConnection(2.seconds)

    case m =>
      stash()
  }

  val waitingForMqttConnection: Receive = MyReceive {

    case ReceiveTimeout =>
      log.info("ReceiveTimeout while waitingForRegisteredMqttConfirm")
      context stop self

    case Registered =>
      log.info("Registered to mqttConnector!!!")
      context.setReceiveTimeout(Duration.Undefined)
      updateState(MqttConnected(true))
      val snd = sender()
      context watch snd
      mqttConnectorActorOpt = Some(snd)
      unstashAll()
      context become waitingForRobotActorCreate

    case m => stash()
  }

  val waitingForRobotActorCreate: Receive = MyReceive {

    case Create(customer) =>
      persist(RobotCreated(customer)) { evt =>
        updateState(evt)
        robotRegistrySupervisor ! RegisterRobot(state.uuid, customer)
        context become waitingForRegistryConfirm(sender())
      }

    case m: MqttPayload =>
      //TODO maybe signal inconsistent state in actor state with a transient property
      log.warning(s"ignore mqtt message $m while waiting for initialization")

    case _ =>
      sender() ! protocol.RobotActor.NotInitialized
  }

  def waitingForRegistryConfirm(respondTo: ActorRef): Receive = MyReceive {

    case RegistrationConfirmed =>
      respondTo ! Initialized(Robot(state.uuid, state.customer.get))
      unstashAll()
      context become waitingForConfigureByDevice

    case DeregistrationConfirmed =>
      respondTo ! Unregistered
      unstashAll()
      context become removed

    case m => stash()
  }

  val waitingForConfigureByDevice: Receive = MyReceive {

    case Configure(msgUuid, locationMap) =>
      log.debug(s"command Configure for robot=${state.uuid}")
      log.debug(s"json message=${toJson(Configure(msgUuid, locationMap))}")
      val pngMap = Configure(msgUuid, locationMap).getPng
      mapWidth = pngMap.getWidth
      mapHeight = pngMap.getHeight
      ImageIO.write(pngMap, "PNG", new File(s"$locationMapFolder${File.separator}$uuidStr.png"))
      persist(RobotConfigured(uuidStr)) { evt =>
        updateState(evt)
        sender() ! Ack(Some(msgUuid))
      }

    case _ =>
      log.debug("RobotNotConfigures - Waiting for first connection")
      sender() ! WrongRobotState("Waiting for first connection")
  }

  val working: Receive = MyReceive {

    case protocol.RobotActor.Remove =>
      log.debug(s"command RemoveRobot uuid=$uuidStr")
      persist(RobotRemoved) { evt =>
        updateState(evt)
        robotRegistrySupervisor ! DeregisterRobot(state.uuid)
        context become waitingForRegistryConfirm(sender())
      }

    case KeepAlive =>
      log.debug("received KeepAlive")
      log.debug(s"json message=${toJson(KeepAlive)}")
      updateState(AliveReceived)
      sender ! Alive

    case ReceiveTimeout =>
      log.debug("missed KeepAlive")
      persist(AliveMissed) { evt =>
        if (state.missedAlive <= 3)
          context.setReceiveTimeout(keepAliveTimeout)
        else
          context.stop(self)
      }

    case Terminated(_) =>
      log.info("mqttConnector Terminated")
      mqttConnectorActorOpt.foreach(context.unwatch)
      mqttConnectorActorOpt = None
      updateState(MqttConnected(false))
      tryMqttConnection(3.seconds)

    case Position(robotId, x, y, theta) =>
      log.debug(s"json message=${toJson(Position(robotId, x, y, theta))}")
      log.info(s"Position $robotId x=$x, y=$y, theta=$theta")
      if (robotId != UUID.fromString(uuidStr)) {
        log.error(s"Position for wrong robot")
      } else {
        context.system.eventStream.publish(Position(robotId, x / mapWidth, y / mapHeight, theta))
      }

    case protocol.RobotActor.WatchedBy(operatorId) =>
      log.info(s"WatchedBy $operatorId")
      mqttConnectorActorOpt.foreach(_ ! mqtt.protocol.WatchedBy(operatorId))

    case m: FallenPerson =>
      log.debug(s"json message=${toJson(FallenPerson(m.priority, m.messageUuid, m.robotId, m.x, m.y, m.timestamp))}")
      log.info(s"FallenPerson robotId=${m.robotId} messageId=${m.messageUuid} x=${m.x}, y=${m.y}, timestamp=${m.timestamp}")
      if (m.robotId != UUID.fromString(uuidStr)) {
        log.error(s"FallenPerson for wrong robot")
      } else {
        val eventToPersist = toMqttMessageEvent(m)
        persist(eventToPersist) { evt =>
          env.mqttMessageDao.insert(eventToPersist)
          sender() ! Ack(Some(m.messageUuid))
        }
      }

    case m: DetectedPerson =>
      log.debug(s"json message=${toJson(DetectedPerson(m.priority, m.messageUuid, m.robotId, m.x, m.y, m.personId, m.timestamp))}")
      log.info(s"DetectedPerson robotId=${m.robotId} x=${m.x}, y=${m.y}, personId=${m.personId} timestamp=${m.timestamp}")
      if (m.robotId != UUID.fromString(uuidStr)) {
        log.error(s"DetectedPerson for wrong robot")
      } else {
        val eventToPersist = toMqttMessageEvent(m)
        persist(eventToPersist) { evt =>
          env.mqttMessageDao.insert(eventToPersist)
          //TODO for now not ack
        }
      }

    case m: RecognizedPerson =>
      log.debug(s"json message=${toJson(RecognizedPerson(m.priority, m.messageUuid, m.robotId, m.nome, m.personId, m.timestamp))}")
      log.info(s"RecognizedPerson robotId=${m.robotId} nome=${m.nome}, personId=${m.personId} timestamp=${m.timestamp}")
      if (m.robotId != UUID.fromString(uuidStr)) {
        log.error(s"RecognizedPerson for wrong robot")
      } else {
        val eventToPersist = toMqttMessageEvent(m)
        persist(eventToPersist) { evt =>
          env.mqttMessageDao.insert(eventToPersist)
          //TODO for now not ack
        }
      }

    case m: BatteryLevel =>
      log.debug(s"json message=${toJson(BatteryLevel(m.priority, m.messageUuid, m.robotId, m.timestamp))}")
      log.info(s"BatteryLevel robotId=${m.robotId} messageId=${m.messageUuid}, timestamp=${m.timestamp}")
      if (m.robotId != UUID.fromString(uuidStr)) {
        log.error(s"BatteryLevel for wrong robot")
      } else {
        val eventToPersist = toMqttMessageEvent(m)
        persist(eventToPersist) { evt =>
          env.mqttMessageDao.insert(eventToPersist)
          sender() ! Ack(Some(m.messageUuid))
        }
      }

    case protocol.RobotActor.MovedTo(x, y) => {
      println("*" * 180)
      log.info(s"MovedTo $x $y")
      mqttConnectorActorOpt.foreach(_ ! mqtt.protocol.MovedTo(x, y))
    }

    case m => log.error(s"working - Unhandled message $m")
  }

  private def toMqttMessageEvent(m: MqttPayload) = {
    MqttMessageEvent(m.header, m.toJson, Instant.now(Clock.systemUTC()).toEpochMilli)
  }

  val removed: Receive = MyReceive {
    case m =>
      log.warning("robot in removed state")
      unhandled(m)
  }

  val receiveRecover: Receive = LoggingReceive {

    case event: Event => {
      log.info(s"Orobot recovering: uuid:${state.uuid} event:$event")
      updateState(event)
    }

    case SnapshotOffer(_, snapshot: State) => state = snapshot

    case RecoveryCompleted => {
      log.info(s"Orobot recovery completed uuid:${state.uuid} state: $state")
    }

    case _ => log.error("receiveRecover- Unhandled message")
  }

  def updateState(evt: Event): Unit = evt match {

    case RobotCreated(customer) =>
      state = state.updated(evt)
      env.robotDao.insert(Robot(state.uuid, state.customer.get))
      context become waitingForConfigureByDevice

    case RobotRemoved =>
      state = state.updated(evt)
      context become removed

    case r: RobotConfigured =>
      state = state.updated(evt)
      context become working

    case e => state = state.updated(e)

  }

  private def tryMqttConnection(delay: FiniteDuration) = {
    import context.dispatcher
    context.system.scheduler.scheduleOnce(delay) {
      mqttConnectorSupervisorActorSelection ! Identify(None)
    }
    context become waitingForMqttActorIdentity
  }

  private def alodProxy(): ActorRef = {
    val target = context.actorSelection(env.robotRegistrySupervisorPath)
    val proxyPersistenceId = classOf[ALODProxyActor].getSimpleName + "_" + uuidStr
    context.actorOf(ALODProxyActor.props(target, proxyPersistenceId))
  }

  context become waitingForMqttActorIdentity
}

