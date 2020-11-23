package io.cinvestav

import java.sql.Timestamp

import cats.data.Kleisli
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import fs2.kafka.{KafkaProducer, ProducerRecord, ProducerRecords, ProducerSettings, produce, producerStream}
import fs2.{Pipe, Stream}
import io.cinvestav.config.SensorConfig
import org.slf4j.LoggerFactory
import pureconfig.ConfigReader.Result
import pureconfig.generic.auto._
import pureconfig._
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random
import java.time.Instant


trait Event
case class SensorEvent(value:Double,timestamp: Long) extends Event

object Sensor extends IOApp{
  val config: Result[SensorConfig] = ConfigSource.default.load[SensorConfig]
  final val logger = LoggerFactory.getLogger("sensor")

  override def run(args: List[String]): IO[ExitCode] = {
    val  TOPIC_NAME:String  = "sensor"
    val BATCH_SIZE:Int = 1000
    val TIME_WINDOW:FiniteDuration = 30 seconds

    def generateRandomNumber(start:Double,end:Double)=
      Random.nextDouble() *(start-end) +end


    val startStream:Kleisli[IO,SensorConfig,Stream[IO,_]] =
      Kleisli(
        (config:SensorConfig)=>{
          //    Producer settings
        val producerSettings = ProducerSettings[IO,Option[String],String]
          .withBootstrapServers(config.bootstrapServers)
//
          producerStream[IO]
            .using(producerSettings)
            .flatMap {
              producer=>Stream.emit(1)
                .repeat.covary[IO]
                .evalTap(_=>logger.info(s"${config.sensorId} is sending...").pure[IO])
                .evalMap(_=>Instant.now().getEpochSecond.pure[IO])
                .evalMap(timestamp => SensorEvent(generateRandomNumber(0,10),timestamp).pure[IO])
                .evalMap(sensorEvent=>sensorEvent.asJson.pure[IO])
                .evalTap(x=>logger.info(x.toString()).pure[IO])
                .evalMap(x=>ProducerRecord(TOPIC_NAME,None,x.toString).pure[IO])
                .evalMap(ProducerRecords.one(_).pure[IO])
                .evalMap(producer.produce)
                .groupWithin(BATCH_SIZE,TIME_WINDOW)
                .metered(5 seconds)
            }
            .pure[IO]
        }
      )

    config match {
      case Left(value) =>
        logger.error(s"Config file is wrong: ${value.head.description}")
        IO.unit.as(ExitCode.Error)
      case Right(value) =>
        val response  = startStream.run(value).unsafeRunSync()
        response.compile.drain.as(ExitCode.Success)
    }
  }
}
