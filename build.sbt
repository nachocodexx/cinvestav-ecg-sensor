name := "cinvestav-ecg-sensor"
version := "0.1"
scalaVersion := "2.13.3"
resolvers += "confluent" at "https://packages.confluent.io/maven/"
libraryDependencies ++= Seq(
  "com.github.fd4s" %% "fs2-kafka" % "1.1.0",
  "com.github.fd4s" %% "fs2-kafka-vulcan" % "1.1.0",
  "org.typelevel" %% "cats-core" % "2.1.1",
  "co.fs2" %% "fs2-core" % "2.4.4",
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)