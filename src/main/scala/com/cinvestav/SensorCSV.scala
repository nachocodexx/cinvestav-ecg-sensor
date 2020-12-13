package com.cinvestav

import cats.effect.{Blocker, Concurrent, ContextShift, IO, Sync, Timer}
import fs2.kafka.{KafkaProducer, ProducerRecord, ProducerRecords}
import fs2.{io, text}
import cats.implicits._
import cats.effect.implicits._

import java.nio.file.Paths
import java.time.Instant
import scala.concurrent.duration._
import scala.language.postfixOps
import com.cinvestav.config.SensorConfig
import com.cinvestav.events.SensorEvent
import com.cinvestav.serializers.SensorKey

import java.util.Calendar


object SensorCSV {
  def apply[F[_]:Sync:Concurrent:ContextShift:Timer](path:String, producer:KafkaProducer.Metrics[F,SensorKey,
    SensorEvent])(implicit config:SensorConfig): F[Unit] = {
    Blocker[F].use { blocker=>
      io.file.readAll[F](Paths.get(path), blocker, 4096)
        .through(text.utf8Decode)
        .through(text.lines)
        .drop(1)
        .map(_.split(",").toList.tail.head)
        .evalMap{
          x=>
            val timestamp = Instant.now().getEpochSecond
            val value =SensorEvent(config.sensorId,x.toDouble,timestamp)
            val key = SensorKey("sid",config.sensorId)
            val record = ProducerRecord(config.topicName,key,value)
            ProducerRecords.one(record).pure[F]
        }
        .evalMap(producer.produce)
        .groupWithin[F](10,1 seconds)
//        .metered[F](100 milliseconds)
//        .debug(x=>s" [INFO] ${x.}")
        .debug(chunk=>s"[${Calendar.getInstance().getTime}] [INFO] - Sensor[${config.sensorId}] - Chuck Size[${chunk
          .size}]")
        .repeat
        .compile
        .drain
    }
  }

}
