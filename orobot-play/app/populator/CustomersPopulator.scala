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
package populator

import java.io.File
import java.util.UUID
import javax.inject.{ Inject, Named, Singleton }

import akka.actor.{ ActorRef, ActorSystem }
import elastic.json.{ AddressJson, CustomerJson }
import model.{ Address, Customer, Robot }
import play.api.{ Application, Configuration, Logger }
import protocol.RobotActor.{ Create, Initialized }
import protocol.RobotsSupervisor.Cmd4Robot

import scala.io.BufferedSource
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

@Singleton
class CustomersPopulator @Inject() (
    configuration: Configuration,
    @Named("orobotplay") orobotplay: ActorRef,
    val system: ActorSystem)(app: Application)(implicit ec: ExecutionContext) extends CustomerJson with AddressJson {

  val populate = {
    val executeBatchImport = app.configuration.getBoolean("customers.import.onstart").getOrElse(false)
    Logger.info(s"populator.CustomersPopulator executeBatchImport=$executeBatchImport")
    if (executeBatchImport) {
      val batchCsvPath = app.configuration.getString("customers.import.file.csv").getOrElse("./customers.csv")
      val customersToCreate: Set[(UUID, Customer)] = readCustomerFromFile(batchCsvPath)
      Logger.info(s"Starting import of ${customersToCreate.size} customers from $batchCsvPath")
      Thread.sleep(3000)
      val testCustomers = customersToCreate map {
        c =>
          implicit val timeout: Timeout = 5.seconds
          (orobotplay ? Cmd4Robot(c._1, Create(c._2))) map {
            case Initialized(robot: Robot) => Logger.debug(s"Created robot ${c._1}")
            case _ => Logger.debug("not created")
          }
      }
      Logger.debug(s"Created robost $testCustomers")
    }
  }

  private def readCustomerFromFile(path: String): Set[(UUID, Customer)] = {
    var customers: Set[(UUID, Customer)] = Set.empty
    import scala.io.Source
    try {
      val bufferedSource: BufferedSource = Source.fromFile(new File(path))
      for (line <- bufferedSource.getLines.drop(1)) {
        val cols = line.split(",").map(_.trim)
        val c = Customer(cols(0),
          cols(1),
          cols(2),
          cols(3),
          Address(cols(4), cols(5), cols(6), cols(7)),
          cols(8))
        customers = customers + (UUID.fromString(cols(9)) -> c)
      }
      bufferedSource.close
    } catch {
      case ex: Exception => Logger.error(s"Error during customers load", ex)
    }
    customers
  }
}
