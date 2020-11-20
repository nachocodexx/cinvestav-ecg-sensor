package io.cinvestav
import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.functor._
import fs2.Pipe
import fs2.kafka._
//import fs2.kafka.vulcan
import org.slf4j.LoggerFactory
import scala.concurrent.duration._

object Main extends IOApp {
  final val log= LoggerFactory.getLogger("app")
  override def run(args: List[String]): IO[ExitCode] = {

    def processRecord(record: ConsumerRecord[Option[String], String]): IO[Unit] =
      IO(log.info(s"PROCESS RECORD: $record"))

    val consumerSettings = ConsumerSettings(
      keyDeserializer = Deserializer[IO,Option[String]],
      valueDeserializer = Deserializer[IO,String]
    )
      .withAutoOffsetReset(AutoOffsetReset.Latest)
        .withBootstrapServers("localhost:9092")
        .withGroupId("group")


    val printPipe:Pipe[IO,CommittableConsumerRecord[IO,String,String],CommittableConsumerRecord[IO,String,String]] =
            in=> {
              println("HOLA")
              in
            }
    val stream = consumerStream[IO].using(consumerSettings)
                .evalTap(_.subscribeTo("mytopic"))
                .flatMap(_.stream)
      .mapAsync(25)(x=>processRecord(x.record))


    stream.compile.drain.as(ExitCode.Success)
  }
}

