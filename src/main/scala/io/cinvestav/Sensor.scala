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

import cats.effect.concurrent.{MVar, Ref}


trait Event
case class SensorEvent(sensorId:String,measurement:Double,timestamp: Long) extends Event

object Sensor extends IOApp{
  val config: Result[SensorConfig] = ConfigSource.default.load[SensorConfig]
  final val logger = LoggerFactory.getLogger("sensor")
  type AppContext = (MVar[IO, Unit], SensorConfig)

  override def run(args: List[String]): IO[ExitCode] = {
    val BATCH_SIZE:Int = 5
    val TIME_WINDOW:FiniteDuration =  100 milliseconds
    val RATE_TIME = 1 seconds

    def generateRandomNumber(start:Double,end:Double)=
      Random.nextDouble() *(start-end) +end


    val startStream:Kleisli[IO,SensorConfig,Stream[IO,_]] =
      Kleisli(
        (config:SensorConfig)=>{
          //    Producer settings
        val producerSettings = ProducerSettings[IO,Option[String],String]
          .withBootstrapServers(config.bootstrapServers)
//        val restartStream  = Stream.eval(MVar.empty[IO,Unit])
//          restartStream.flatMap {
//            restart=>
              val vitalSignRef = Ref.of[IO,Double](generateRandomNumber(1,10)).unsafeRunSync()
//              //////////////////////////////////////////////
          producerStream[IO]
            .using(producerSettings)
            .flatMap {
              producer=>Stream.emit(1)
                .repeat.covary[IO]
//                .evalTap(_=>logger.info(s"${config.sensorId} is sending...").pure[IO])
                .evalMap(_=>Instant.now().getEpochSecond.pure[IO])
//                .evalMap(_=>(vitalSign.flatMap(_.take),_))
//                .evalMap(timestamp => SensorEvent(generateRandomNumber(0,10),timestamp).pure[IO])
                .evalMap(timestamp =>
                  for{
                    vitalSign<-vitalSignRef.get
                    event <- SensorEvent(config.sensorId,2+vitalSign,timestamp).pure[IO]
                  } yield event
                )
                .evalMap(sensorEvent=>sensorEvent.asJson.pure[IO])
                .evalTap(x=>logger.info(x.toString()).pure[IO])
                .evalMap(x=>ProducerRecord(config.topicName,Some("sid:"+config.sensorId),x.toString).pure[IO])
                .evalMap(ProducerRecords.one(_).pure[IO])
                .evalMap(producer.produce)
                .groupWithin(BATCH_SIZE,TIME_WINDOW)
                .concurrently(
                  Stream
                    .iterate(1)(_+1)
                    .covary[IO]
//                    .interruptWhen(restart.take.attempt)
                    .repeat
//                    .evalTap(_=>println("NEW VALUE").pure[IO])
//                    .evalMap(_=>restart.put(()))
                    .evalTap(_=>
                      for {
                        amount<- generateRandomNumber(-1,1).pure[IO]
                        _<- vitalSignRef.modify(x=>(amount,x))
                      } yield ()
                    )
                    .metered(1 millisecond)
                )
                .metered(RATE_TIME)
            }
          }
            .pure[IO]
//        }
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
