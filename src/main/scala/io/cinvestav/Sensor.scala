package io.cinvestav

import java.sql.Timestamp
import cats.effect.{ExitCode, IO, IOApp},cats.implicits._
import fs2.kafka.{KafkaProducer, ProducerRecord, ProducerRecords, ProducerSettings, produce, producerStream}
import fs2.{Pipe, Stream}
import org.slf4j.LoggerFactory
import scala.concurrent.duration._
import scala.language.postfixOps

object Sensor extends IOApp{
  final val logger = LoggerFactory.getLogger("sensor-01")

  override def run(args: List[String]): IO[ExitCode] = {
    val BOOTSTRAP_SERVERS:String = "localhost:9092"

    //    Producer settings
    val producerSettings = ProducerSettings[IO,Option[String],String]
      .withBootstrapServers(BOOTSTRAP_SERVERS)


    val produceFakeData:Pipe[IO,KafkaProducer.Metrics[IO,Option[String],String],Int] =
      in=> Stream
        .emit(1)
        .evalTap(_=>IO(logger.info("SENDING FAKE DATA...")))
        .metered(2 seconds)

    val createProducerData = Stream.emit(1).repeat.covary[IO]
      .evalTap(_=>IO(logger.info("SENDING FAKE DATA...")))
      .metered(2 seconds)
      .compile.drain

    val stream = producerStream[IO]
      .using(producerSettings)
      .flatMap {
        producer => Stream.emit(1)
          .repeat.covary[IO]
          .evalTap(_=>logger.info("SENT DATA...").pure[IO])
          .evalMap(_=>new Timestamp(System.currentTimeMillis).pure[IO])
          .evalMap(x=>ProducerRecord("mytopic",None,s"Freddy Goto - $x").pure[IO])
          .evalMap(ProducerRecords.one(_).pure[IO])
          .evalMap(producer.produce)
          .groupWithin(100,10 seconds)
          .metered(10 seconds)
      }

    val response = stream.compile.drain.as(ExitCode.Success)
    response
  }
}
