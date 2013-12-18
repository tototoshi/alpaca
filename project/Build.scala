import sbt._
import Keys._
import com.typesafe.sbt.SbtStartScript

object AlpacaBuild extends Build {

  lazy val _version = "0.1.1" 

  lazy val root = Project(
    id = "root",
    base = file(".")
  ).aggregate(lang)

  val baseDependency = Seq(
    "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
    "ch.qos.logback" % "logback-classic" % "1.0.13",
    "commons-io" % "commons-io" % "2.4",
    "org.scalatest" % "scalatest_2.10" % "2.0" % "test"
  )

  lazy val lang = Project (
    id = "lang",
    base = file ("lang"),
    settings = Defaults.defaultSettings ++ Seq (
      name := "alpaca",
      organization := "com.github.tototoshi",
      version := _version,
      scalaVersion := "2.10.2",
      resolvers += Resolver.sonatypeRepo("public"),
      libraryDependencies ++= baseDependency ++ Seq(
      "org.seleniumhq.selenium" % "selenium-java" % "2.38.0",
      "com.github.scopt" %% "scopt" % "3.2.0"
      ),
      scalacOptions ++= Seq("-deprecation", "-language:_")
    ) ++ SbtStartScript.startScriptForClassesSettings
  )

  lazy val test = Project (
    id = "testkit",
    base = file ("testkit"),
    settings = Defaults.defaultSettings ++ Seq (
      name := "alpaca-test",
      organization := "com.github.tototoshi",
      version := _version,
      scalaVersion := "2.10.2",
      libraryDependencies ++= baseDependency
    )
  )

}
