name := "scalabeo"

version := "0.1"

scalaVersion := "2.12.9"

val akkaVersion = "2.5.25"


// Determine OS version of JavaFX binaries
lazy val osName = System.getProperty("os.name") match {
  case n if n.startsWith("Linux")   => "linux"
  case n if n.startsWith("Mac")     => "mac"
  case n if n.startsWith("Windows") => "win"
  case _ => throw new Exception("Unknown platform!")
}


libraryDependencies ++= Seq(
  "org.scalafx" %% "scalafx" % "12.0.1-R17",
  "io.github.typhon0" % "AnimateFX" % "1.2.1",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.1.1" % "test",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
  "eu.hansolo" % "Medusa" % "8.3",
  "org.openjfx" % s"javafx-base" % "12.0.1" classifier osName,
  "org.openjfx" % s"javafx-controls" % "12.0.1" classifier osName,
  "org.openjfx" % s"javafx-fxml" % "12.0.1" classifier osName,
  "org.openjfx" % s"javafx-graphics" % "12.0.1" classifier osName,
  "org.openjfx" % s"javafx-media" % "12.0.1" classifier osName,
  "org.openjfx" % s"javafx-swing" % "12.0.1" classifier osName,
  "org.openjfx" % s"javafx-web" % "12.0.1" classifier osName
)