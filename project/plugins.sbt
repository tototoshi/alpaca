lazy val lang = project.in(file(".")).dependsOn(nativePackager)
lazy val nativePackager = uri("https://github.com/tototoshi/sbt-native-packager.git#b2de9baf8f9fcec99e841c5f3b7ec98ed271fc8c")

addSbtPlugin("com.typesafe.sbt" % "sbt-start-script" % "0.10.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.2.1")

