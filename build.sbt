name := "scalabeo"

version := "0.1"

scalaVersion := "2.12.9"

val akkaVersion = "2.5.25"

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
  "com.typesafe.akka" %% "akka-testkit" % "2.6.3" % "test"
)