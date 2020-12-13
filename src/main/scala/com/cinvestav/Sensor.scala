package com.cinvestav
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.cinvestav.config.SensorConfig
import org.slf4j.LoggerFactory
import pureconfig.ConfigReader.Result
import pureconfig.generic.auto._
import pureconfig._
import scala.language.postfixOps

object Sensor extends IOApp{
  val config: Result[SensorConfig] = ConfigSource.default.load[SensorConfig]
  final val logger = LoggerFactory.getLogger("sensor")

  override def run(args: List[String]): IO[ExitCode] = {
    config match {
      case Left(error) =>
        logger.error(s"Config file is wrong: ${error.head.description}")
        IO.unit.as(ExitCode.Error)
      case Right(value) =>
        implicit  val config: SensorConfig = value
        val path = s"${value.ecgDataPath}/${value.ecgDataFilename}"

        Producer[IO]().evalMap(SensorCSV[IO](path,_))
          .compile
          .drain
          .as(ExitCode.Success)

    }
  }
}
