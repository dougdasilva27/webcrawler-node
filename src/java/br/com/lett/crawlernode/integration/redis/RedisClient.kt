package br.com.lett.crawlernode.integration.redis

import br.com.lett.crawlernode.main.GlobalConfigurations.executionParameters
import org.slf4j.LoggerFactory
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import kotlin.time.*

@ExperimentalTime
object RedisClient {
   private val logger = LoggerFactory.getLogger(RedisClient::class.java)

   private val pool: JedisPool by lazy {
      JedisPool(JedisPoolConfig(), executionParameters.redisHost, executionParameters.redisPort)
   }
   private const val logPrefix = "[Redis]"

   fun getKey(key: String): String? {
      val timedValue: TimedValue<String?> = measureTimedValue { pool.resource.get(key) }
      logger.debug("$logPrefix [Get] ${timedValue.duration.inMilliseconds} ms")
      return timedValue.value
   }

   fun setExKey(key: String, value: String, seconds: Int) {
      val duration: Duration = measureTime { pool.resource.setex(key, seconds, value) }
      logger.debug("$logPrefix [SetEx] ${duration.inMilliseconds} ms")
   }

   fun setKey(key: String, value: String) {
      val duration: Duration = measureTime { pool.resource.set(key, value) }
      logger.debug("$logPrefix [Set] ${duration.inMilliseconds} ms")
   }
}
