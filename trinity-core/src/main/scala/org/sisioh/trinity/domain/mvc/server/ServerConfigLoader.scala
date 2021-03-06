/*
 * Copyright 2013 Sisioh Project and others. (http://sisioh.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.sisioh.trinity.domain.mvc.server

import java.io.{FileNotFoundException, File}
import java.net.InetSocketAddress
import org.sisioh.config.{ConfigurationMode, Configuration}
import org.sisioh.trinity.domain.mvc.Environment
import scala.concurrent.duration.Duration
import scala.util.{Failure, Try}

object ServerConfigLoader extends ServerConfigLoader

/**
 * Represents the service loading the configuration file.
 */
class ServerConfigLoader {

  /**
   * Loads the configuration to be specified by the application and the environment.
   *
   * @param applicationId application id
   * @param environment [[Environment.Value]]
   * @param serverConfigEventListener [[ServerConfigEventListener]]
   * @return wrapped [[org.sisioh.config.Configuration]] around `scala.util.Try`
   */
  def loadConfiguration(applicationId: String,
                        environment: Environment.Value,
                        serverConfigEventListener: Option[ServerConfigEventListener] = None): Try[Configuration] = Try {
    val configuration = Configuration.loadByMode(
      new File(applicationId),
      if (environment == Environment.Product)
        ConfigurationMode.Prod
      else
        ConfigurationMode.Dev
    )
    serverConfigEventListener.foreach {
      el =>
        el.onLoadedConfig(configuration)
    }
    configuration
  }.recoverWith {
    case ex =>
      Failure(
        new FileNotFoundException(
          "Configuration file is not found." +
            "please set config path to -Dconfig.file or -Dconfig.resource, -Dconfig.url. " +
            s"(applicationId = $applicationId, environment = $environment)"
        )
      )
  }

  private def getKeyName(keyName: String, prefix: Option[String] = None) =
    prefix.fold(keyName)(_ + keyName)

  /**
   * Loads the OpenConnectionsThresholds configuration items.
   *
   * @param configuration `Configuration`
   * @param prefix prefix string
   * @return wrapped [[OpenConnectionsThresholdsConfig]] around `scala.Option`
   */
  protected def loadOpenConnectionsThresholdsConfig
  (configuration: Configuration, prefix: Option[String]): Option[OpenConnectionsThresholdsConfig] = {
    configuration.getConfiguration(getKeyName("openConnectionThresholds", prefix)).map {
      c =>
        val lowWaterMark = c.getIntValue("lowWaterMark").get
        val highWaterMark = c.getIntValue("highWaterMark").get
        val idleTime = c.getStringValue("idleTime").map(Duration(_)).get
        OpenConnectionsThresholdsConfig(lowWaterMark, highWaterMark, idleTime)
    }
  }

  /**
   * Loads the Tls configuration items.
   *
   * @param configuration `Configuration`
   * @param prefix prefix string
   * @return wrapped [[TlsConfig]] around `scala.Option`
   */
  protected def loadTlsConfig(configuration: Configuration, prefix: Option[String]): Option[TlsConfig] = {
    configuration.getConfiguration(getKeyName("tls", prefix)).map {
      c =>
        val certificatePath = c.getStringValue("certificatePath").get
        val keyPath = c.getStringValue("keyPath").get
        val caCertificatePath = c.getStringValue("caCertificatePath")
        val ciphers = c.getStringValue("ciphers")
        val nextProtos = c.getStringValue("nextProtos")
        TlsConfig(certificatePath, keyPath, caCertificatePath, ciphers, nextProtos)
    }
  }

  /**
   * Loads the configuration as [[ServerConfig]].
   *
   * @param configuration `Configuration`
   * @param prefix prefix string
   * @return [[ServerConfig]]
   */
  def loadAsServerConfig(configuration: Configuration, prefix: Option[String] = None): ServerConfig = {
    val serverConfiguration = ServerConfig(
      name = configuration.
        getStringValue(getKeyName("name", prefix)),
      bindAddress = configuration.
        getStringValue(getKeyName("bindAddress", prefix)).map {
        bindAddress =>
          val splits = bindAddress.split(":")
          if (splits.size > 1) {
            val host = splits(0)
            val port = splits(1).toInt
            new InetSocketAddress(host, port)
          } else {
            new InetSocketAddress(bindAddress.toInt)
          }
      },
      statsEnabled = configuration.getBooleanValue(getKeyName("stats.enabled", prefix)).getOrElse(false),
      statsPort = configuration.getIntValue(getKeyName("stats.port", prefix)),
      maxRequestSize = configuration.getIntValue(getKeyName("maxRequestSize", prefix)),
      maxResponseSize = configuration.getIntValue(getKeyName("maxResponseSize", prefix)),
      maxConcurrentRequests = configuration.getIntValue(getKeyName("maxConcurrentRequests", prefix)),
      hostConnectionMaxIdleTime = configuration.getStringValue(getKeyName("hostConnectionMaxIdleTime", prefix)).map(Duration(_)),
      hostConnectionMaxLifeTime = configuration.getStringValue(getKeyName("hostConnectionMaxLifeTime", prefix)).map(Duration(_)),
      requestTimeout = configuration.getStringValue(getKeyName("requestTimeout", prefix)).map(Duration(_)),
      readTimeout = configuration.getStringValue(getKeyName("readTimeout", prefix)).map(Duration(_)),
      writeCompletionTimeout = configuration.getStringValue(getKeyName("writeCompletionTimeout", prefix)).map(Duration(_)),
      sendBufferSize = configuration.getIntValue(getKeyName("sendBufferSize", prefix)),
      receiveBufferSize = configuration.getIntValue(getKeyName("receiveBufferSize", prefix)),
      newSSLEngine = None,
      tlsConfig = loadTlsConfig(configuration, prefix),
      openConnectionsThresholdsConfig = loadOpenConnectionsThresholdsConfig(configuration, prefix)
    )
    serverConfiguration
  }

}
