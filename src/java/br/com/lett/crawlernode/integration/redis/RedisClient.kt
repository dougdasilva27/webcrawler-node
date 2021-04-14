package br.com.lett.crawlernode.integration.redis

import br.com.lett.crawlernode.main.GlobalConfigurations.executionParameters
import org.redisson.Redisson
import org.redisson.api.RMapCache
import org.redisson.api.RedissonClient
import org.redisson.config.Config

/**
 * Singleton synchronous redis client. It is lazily evaluated, won't connect to Redis until being used.
 *
 * @see RedisClient
 * @see CrawlerCache
 *
 * @author Charles Fonseca
 */
object RedisClient {

   private val client: RedissonClient by lazy {
      val config = Config()
      config.useSingleServer()
         .setAddress("redis://${executionParameters.redisHost}:${executionParameters.redisPort}")
         .timeout = 10000
      Redisson.create(config)
   }

   val crawlerCache: RMapCache<String, Any?> by lazy {
      client.getMapCache("Crawler")
   }

   fun shutdown() {
      client.shutdown()
   }
}
