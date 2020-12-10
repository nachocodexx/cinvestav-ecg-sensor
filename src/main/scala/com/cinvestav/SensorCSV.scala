package com.cinvestav

import cats.effect.{Blocker, Concurrent, ContextShift, IO, Sync, Timer}
import fs2.kafka.KafkaProducer
import fs2.{io, text}
import cats.implicits._,cats.effect.implicits._
import java.nio.file.Paths
import java.time.Instant
import scala.concurrent.duration._
import scala.language.postfixOps
import com.cinvestav.config.SensorConfig


object SensorCSV {
  def apply[F[_]:Sync:Concurrent:ContextShift:Timer](path:String, producer:KafkaProducer.Metrics[F,Option[String],
    String])(implicit config:SensorConfig)= {
    Blocker[F].use { blocker=>
      io.file.readAll[F](Paths.get(path), blocker, 4096)
        .through(text.utf8Decode)
        .through(text.lines)
        .drop(1)
        .map(_.split(",").toList.tail.head)
        .evalMap{
          value=>
            val timestamp = Instant.now().getEpochSecond
            SensorEvent(config.sensorId,value.toLong,timestamp).pure[F]
        }
        .metered[F](100 milliseconds)
        .groupWithin[F](10,1 seconds)
        .debug()
        .repeat
        .compile
        .drain
    }
  }

}
