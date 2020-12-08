lazy val PureConfig = "com.github.pureconfig" %% "pureconfig" % "0.14.0"
lazy val fs2KafkaVersion = "1.1.0"

lazy val CatsEffect= "org.typelevel" %% "cats-core" % "2.1.1"

val circeVersion = "0.12.3"

lazy val Circe = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

lazy val Fs2Kafka = Seq(
  "com.github.fd4s" %% "fs2-kafka",
  "com.github.fd4s" %% "fs2-kafka-vulcan",
).map(_%fs2KafkaVersion)
lazy val Fs2 = "co.fs2" %% "fs2-core" % "2.4.4"
lazy val LogBack ="ch.qos.logback" % "logback-classic" % "1.2.3"
lazy val Vulcan = "com.github.fd4s" %% "vulcan" % "1.2.0"

lazy val sensorApp = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "cinvestav-ecg-sensor",
    version := "0.1",
    scalaVersion := "2.13.3",
    resolvers += "confluent" at "https://packages.confluent.io/maven/",
    libraryDependencies ++= Seq(
      CatsEffect,
      Fs2,
      LogBack,
      PureConfig,
      Vulcan
    ) ++ Fs2Kafka ++ Circe,
  )


lazy val dockerPorts = sys.env.getOrElse("PORT", 9092).asInstanceOf[Int]
dockerExposedPorts := Seq(dockerPorts,9092)
dockerRepository := Some("nachocode")
packageName in Docker := "cinvestav-ecg-sensor"
version in Docker := "latest"
