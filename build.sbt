import com.typesafe.sbt.SbtStartScript

SbtStartScript.startScriptForClassesSettings

name := "alpaca"

organization := "com.github.tototoshi"

scalaVersion := "2.10.2"

version := "0.1.0-SNAPSHOT"

resolvers += Resolver.sonatypeRepo("public")

libraryDependencies ++= Seq(
    "org.seleniumhq.selenium" % "selenium-java" % "2.37.1",
    "com.github.scopt" %% "scopt" % "3.2.0",
    "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
    "ch.qos.logback" % "logback-classic" % "1.0.13",
    "org.scalatest" % "scalatest_2.10" % "2.0" % "test"
)
