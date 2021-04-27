package br.com.lett.crawlernode.integration.redis

import br.com.lett.crawlernode.main.GlobalConfigurations.executionParameters
import org.redisson.Redisson
import org.redisson.api.RMapCache
import org.redisson.api.RedissonClient
import org.redisson.client.RedisConnectionException
import org.redisson.config.Config

/**
 * Singleton synchronous redis client. It is lazily evaluated, won't connect to Redis until being used.
 *
 * @see Redis
 * @see Cache
 *
 */
object Redis {

   internal val client: RedissonClient? by lazy {
      val config = Config()
      config.useSingleServer()
         .setAddress("redis://${executionParameters.redisHost}:${executionParameters.redisPort}")
         .timeout = 10000
      try {
         Redisson.create(config)
      } catch (e: RedisConnectionException) {
         null
      }
   }

   fun shutdown() {
      client?.shutdown()
   }
}

object CacheFactory {
   fun createCache(type: CacheType): RMapCache<String, Any?>? {
      return Redis.client?.getMapCache(type.name)
   }
}

enum class CacheType {
   CRAWLER, RANKING
}
