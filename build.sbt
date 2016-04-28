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
  lazy val commonSettings:Seq[sbt.Setting[_]] = Seq(
  organization := "it.kynetics",
  version := "1.0.0-SNAPSHOT",
  scalaVersion := "2.11.7",

  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),

  testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "junitxml", "console", "fullstacktrace"),

  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oDF"),

  coverageEnabled.in(Test, test) := true//,

//  fork in test := true

) ++ scalariformSettings

import java.io.File

import com.tuplejump.sbt.yeoman.Yeoman
import sbt.IO
import sbt.Keys._

lazy val orobot_backend = (project in file(".")).
  settings(commonSettings: _*).
  aggregate("orobot-akka", "orobot-play")

lazy val `orobot-model` = (project in file("orobot-model")).
  settings(commonSettings: _*).
  settings(
    libraryDependencies += "com.typesafe.play" % "play-json_2.11" % "2.4.6"
  )

lazy val `orobot-akka-protocol` = (project in file("orobot-akka-protocol")).
  dependsOn("orobot-model","orobot-akka-mqtt","orobot-elastic").
  settings(commonSettings: _*)

lazy val `orobot-akka-mqtt` = (project in file("orobot-akka-mqtt")).
  dependsOn("orobot-model").
  configs(IntegrationTest).
  settings(Defaults.itSettings: _*).
  configs(E2eTest).
  settings(inConfig(E2eTest)(Defaults.testSettings): _*).
  settings(commonSettings: _*).
  settings(
    resolvers += "bintray" at "https://dl.bintray.com/iheartradio/maven/",
    libraryDependencies ++= Seq(
      "com.typesafe.play" % "play-json_2.11" % "2.4.6",
      "com.typesafe.akka" %% "akka-actor" % "2.4.2",
      "com.typesafe.akka" %% "akka-slf4j" % "2.4.2",
      "com.typesafe.akka" %% "akka-contrib" % "2.4.2",
      "com.typesafe.akka" %% "akka-testkit" % "2.4.2" % "test",
      "ch.qos.logback" % "logback-classic" % "1.1.3",
      "com.iheart" %% "ficus" % "1.2.1",
      "org.eclipse.paho" % "org.eclipse.paho.client.mqttv3" % "1.0.2",
      "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test"
    ),
    libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test"
  )

lazy val `orobot-akka-persistence` = (project in file("orobot-akka-persistence")).
  dependsOn("orobot-model","orobot-akka-mqtt").
  settings(commonSettings: _*)

lazy val `orobot-elastic` = (project in file("orobot-elastic")).
  dependsOn("orobot-model","orobot-akka-mqtt").
  settings(commonSettings: _*).
  settings(
    resolvers += Resolver.jcenterRepo,
    resolvers += "bintray" at "https://dl.bintray.com/iheartradio/maven/",
    libraryDependencies ++= Seq(
      "com.sksamuel.elastic4s" %% "elastic4s-core" % "2.1.2",
      "com.sksamuel.elastic4s" %% "elastic4s-jackson" % "2.1.2",
      "com.typesafe.play" % "play-json_2.11" % "2.4.6",
      "org.codehaus.groovy" % "groovy-all" % "2.4.5",
      "org.apache.lucene" % "lucene-expressions" % "5.3.1",
      "com.github.spullara.mustache.java" % "compiler" % "0.9.1",
      "net.java.dev.jna" % "jna" % "4.2.1",
      "com.iheart" %% "ficus" % "1.2.1"
    ),
    dependencyOverrides += "org.elasticsearch" % "elasticsearch" % "2.1.1",
    libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test"
  )


lazy val E2eTest = config ("e2e") extend Test

lazy val `orobot-akka` = (project in file("orobot-akka")).
  aggregate("orobot-model","orobot-akka-protocol","orobot-akka-mqtt","orobot-akka-persistence","orobot-elastic").
  dependsOn("orobot-model","orobot-akka-protocol","orobot-akka-mqtt","orobot-akka-persistence","orobot-elastic").
  configs(IntegrationTest).
  settings(Defaults.itSettings: _*).
  configs(E2eTest).
  settings(inConfig(E2eTest)(Defaults.testSettings): _*).
  settings(commonSettings: _*).
  settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.4.2",
      "com.typesafe.akka" %% "akka-persistence" % "2.4.2",
      "com.typesafe.akka" %% "akka-slf4j" % "2.4.2",
      "com.typesafe.akka" %% "akka-persistence-query-experimental" % "2.4.2",
      "com.typesafe.akka" %% "akka-contrib" % "2.4.2",
      "ch.qos.logback" % "logback-classic" % "1.1.3"
    ),

    resolvers += "krasserm at bintray" at "http://dl.bintray.com/krasserm/maven",
    libraryDependencies += "com.github.krasserm" %% "akka-persistence-cassandra-3x" % "0.6",

    resolvers += "dnvriend at bintray" at "http://dl.bintray.com/dnvriend/maven",
//    libraryDependencies += "com.github.dnvriend" %% "akka-persistence-inmemory" % "1.2.2" % "test",
    libraryDependencies += "com.github.dnvriend" %% "akka-persistence-inmemory" % "1.2.2",

//    libraryDependencies ++= Seq(
//      "org.jvnet.mock-javamail" % "mock-javamail" % "1.9" % "test" ,
//      "com.typesafe.play" %% "play-mailer" % "3.0.1"
//    ),
//
//    libraryDependencies += "org.bouncycastle" % "bcprov-jdk15on" % "1.52",
//
//    libraryDependencies += "com.amazonaws" % "aws-java-sdk" % "1.10.32",

    libraryDependencies += "org.eclipse.paho" % "org.eclipse.paho.client.mqttv3" % "1.0.2",

    libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test" //,

//    libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test"

  )


lazy val `orobot-play` = (project in file("orobot-play")).
  dependsOn("orobot-akka").
  dependsOn(`orobot-sim` % "test").
  enablePlugins(PlayScala).
  settings(commonSettings: _*).
  settings(
    resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
    resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/",

    libraryDependencies ++= Seq(
      jdbc,
      cache,
      ws,
      specs2 % Test
    ),
    resolvers += "bintray" at "https://dl.bintray.com/iheartradio/maven/",
    libraryDependencies ++= Seq(
      "com.mohiva" %% "play-silhouette" % "3.0.4",
      "com.mohiva" %% "play-silhouette-testkit" % "3.0.4" % "test",
      "org.webjars" % "bootstrap" % "3.3.6",
      "org.webjars" % "jquery" % "2.2.0",
      "net.codingwell" %% "scala-guice" % "4.0.1",
      "com.iheart" %% "ficus" % "1.2.1",
      "com.adrianhurt" %% "play-bootstrap3" % "0.4.5-P24",
      "ch.qos.logback" % "logback-classic" % "1.1.3",
//      specs2 % Test,
//      cache,
      filters,
      "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test",
      "commons-io" % "commons-io" % "2.5" % "test"
    ),

    resolvers += "jitpack" at "https://jitpack.io",

    libraryDependencies ++= Seq(
      "com.github.sun-opsys" % "doppelauge" % "1.3.5",
      "org.webjars" %% "webjars-play" % "2.4.0-1",
      "org.webjars" % "swagger-ui" % "2.1.4"
    ),

    libraryDependencies ++= Seq(
      "org.java-websocket" % "Java-WebSocket" % "1.3.0" % "test"
    ),

    routesGenerator := InjectedRoutesGenerator,

    coverageExcludedPackages := "<empty>;controllers.Reverse*;router.Routes.*",

    javaOptions in Test += "-Dconfig.file=conf/application.test.conf",

    testOptions in Test += Tests.Cleanup ( () => {
      baseDirectory.value.listFiles().filter(f => f.getName.startsWith("es-test-")) foreach (delete(_))
      baseDirectory.value.listFiles().filter(f => f.getName.startsWith("maps-test-")) foreach (delete(_))
    })

  ).settings((Yeoman.yeomanSettings ++ Yeoman.withTemplates) : _*)

lazy val `orobot-sim` = (project in file("orobot-sim")).
  dependsOn("orobot-akka-mqtt").
  settings(commonSettings: _*).
  settings(
    resolvers += "bintray" at "https://dl.bintray.com/iheartradio/maven/",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.4.2",
      "com.typesafe.akka" %% "akka-slf4j" % "2.4.2",
      "com.typesafe.akka" %% "akka-contrib" % "2.4.2",
      "ch.qos.logback" % "logback-classic" % "1.1.3",
      "com.iheart" %% "ficus" % "1.2.1"
    ),
    libraryDependencies += "org.eclipse.paho" % "org.eclipse.paho.client.mqttv3" % "1.0.2",
    libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test",
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full),
    libraryDependencies += "org.scalafx" %% "scalafxml-core-sfx8" % "0.2.2",
    libraryDependencies += "org.scalafx" % "scalafx_2.11" % "8.0.31-R7"
  )

  def delete(file: File): Unit = {
    if (file.isDirectory) file.listFiles() foreach (delete(_))
    file.delete()
  }
