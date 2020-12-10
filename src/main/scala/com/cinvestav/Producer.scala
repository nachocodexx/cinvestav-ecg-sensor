package com.cinvestav

import cats.effect.{Concurrent, ConcurrentEffect, ContextShift}
import cats.syntax.OptionIdOps
import com.cinvestav.config.SensorConfig
import fs2.kafka.{KafkaProducer, ProducerSettings, producerStream}

object Producer {
  def apply[F[_]:ConcurrentEffect:ContextShift]()(implicit config:SensorConfig): fs2.Stream[F, KafkaProducer.Metrics[F, Option[String], String]] = {
    val settings = ProducerSettings[F,Option[String],String]
      .withBootstrapServers(config.bootstrapServers)
//      .valueSerializer[]
    producerStream[F]
      .using(settings)
//      .evalMap(_.produce)
  }

}
