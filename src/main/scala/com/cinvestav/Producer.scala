package com.cinvestav

import cats.effect.{ConcurrentEffect, ContextShift}
import com.cinvestav.config.SensorConfig
import com.cinvestav.events.SensorEvent
import com.cinvestav.serializers.SensorKey
import fs2.kafka.{KafkaProducer, ProducerSettings, Serializer, producerStream}

object Producer {
  import com.cinvestav.serializers.VulcanSerializer
  def apply[F[_]:ConcurrentEffect:ContextShift]()(implicit config:SensorConfig): fs2.Stream[F, KafkaProducer
  .Metrics[F, SensorKey, SensorEvent]] = {
    val serializers = VulcanSerializer[F]()
    val settings = ProducerSettings[F,SensorKey,SensorEvent](
      keySerializer = serializers.sensorKeySerializer,
      valueSerializer = serializers.sensorEventSerializer
    )
      .withBootstrapServers(config.bootstrapServers)

    producerStream[F]
      .using(settings)
  }

}
