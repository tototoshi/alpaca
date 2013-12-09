import sbt._
import Keys._
import com.typesafe.sbt.SbtStartScript

object AlpacaBuild extends Build {

  lazy val root = Project(
    id = "root",
    base = file(".")
  ).aggregate(lang)

  lazy val lang = Project (
    id = "lang",
    base = file ("lang"),
    settings = Defaults.defaultSettings ++ Seq (
      name := "alpaca",
      organization := "com.github.tototoshi",
      version := "0.1.0-SNAPSHOT",
      scalaVersion := "2.10.2",
      libraryDependencies ++= Seq(
        "org.scalatest" %% "scalatest" % "1.9.1" % "test"
      ),
      resolvers += Resolver.sonatypeRepo("public"),
      libraryDependencies ++= Seq(
      "org.seleniumhq.selenium" % "selenium-java" % "2.37.1",
      "com.github.scopt" %% "scopt" % "3.2.0",
      "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
      "ch.qos.logback" % "logback-classic" % "1.0.13",
      "org.scalatest" % "scalatest_2.10" % "2.0" % "test"
      ),
      scalacOptions ++= Seq("-deprecation", "-language:_")
    ) ++ SbtStartScript.startScriptForClassesSettings
  )

  lazy val test = Project (
    id = "testkit",
    base = file ("testkit"),
    settings = Defaults.defaultSettings ++ Seq (
      name := "alpaca-lang-test",
      organization := "com.github.tototoshi",
      version := "0.1.0-SNAPSHOT",
      scalaVersion := "2.10.2",
      libraryDependencies ++= Seq(
        "org.scalatest" % "scalatest_2.10" % "2.0" % "test"
      )
    )
  )

}
