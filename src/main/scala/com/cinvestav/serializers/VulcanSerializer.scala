package com.cinvestav.serializers
import cats.effect.{IO, Sync}
import com.cinvestav.events.SensorEvent
import fs2.kafka.vulcan.{AvroSettings, SchemaRegistryClientSettings, avroSerializer}
import cats.implicits._,cats.effect.implicits._
import fs2.kafka.RecordSerializer
import vulcan.Codec

final case class SensorKey(prefix:String,value:String)
class VulcanSerializer[F[_]:Sync]{
  implicit val sensorEvent:Codec[SensorEvent] = Codec.record(
    name="SensorEvent",
    namespace = "com.cinvestav"
  ){ field =>
    (
      field("sensorId",_.sensorId),
      field("measurement",_.measurement),
      field("timestamp",_.timestamp)
      ).mapN(SensorEvent)
  }
  implicit val key:Codec[SensorKey] = Codec.record("SensorKey","com.cinvestav"){ field=>
    (field("prefix",_.prefix),field("value",_.prefix)).mapN(SensorKey)
  }

  val avroSettings: AvroSettings[F] = AvroSettings{
      SchemaRegistryClientSettings[F]("http://ec2-3-90-166-46.compute-1.amazonaws.com:8081")
  }
  implicit val sensorEventSerializer:RecordSerializer[F,SensorEvent]=avroSerializer[SensorEvent].using(avroSettings)
  implicit val sensorKeySerializer:RecordSerializer[F,SensorKey]=avroSerializer[SensorKey].using(avroSettings)
}
object VulcanSerializer{
  def apply[F[_]:Sync]()= new VulcanSerializer[F]()

}
