package com.cinvestav.config

case class SensorConfig(
                       sensorId:String,
                       bootstrapServers:String,
                       topicName:String,
                       ecgDataPath:String,
                       ecgDataFilename:String
                       )
