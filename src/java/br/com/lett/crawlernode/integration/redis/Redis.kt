package br.com.lett.crawlernode.integration.redis

import br.com.lett.crawlernode.integration.redis.config.JsonCodec
import br.com.lett.crawlernode.main.GlobalConfigurations.executionParameters
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.sync.RedisCommands
import java.time.Duration

/**
 * Singleton synchronous redis client. It is lazily evaluated, won't connect to Redis until being used.
 *
 */
object Redis {

   internal val client: RedisClient? by lazy {
      val redisUri = RedisURI.Builder.redis(executionParameters.redisHost, executionParameters.redisPort)
         .withTimeout(Duration.ofSeconds(10)).build()

      try {
         RedisClient.create(redisUri)
      } catch (e: Exception) {
         null
      }
   }
}

object CacheFactory {

   fun <T> createCache(clazz: Class<T>): RedisCommands<String, T?>? {
      return Redis.client?.connect(JsonCodec(clazz))?.sync()
   }
}
