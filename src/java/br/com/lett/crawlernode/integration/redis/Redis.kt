package br.com.lett.crawlernode.integration.redis

import br.com.lett.crawlernode.integration.redis.config.RedisDb
import br.com.lett.crawlernode.main.GlobalConfigurations.executionParameters
import org.redisson.Redisson
import org.redisson.api.RMapCache
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.slf4j.LoggerFactory


/**
 * Singleton synchronous redis client. It is lazily evaluated, won't connect to Redis until being used.
 *
 */
object Redis {
   private val logger = LoggerFactory.getLogger(Redis::class.java)

   internal val client: RedissonClient? by lazy {
      val config = Config()
      config.useSingleServer().setAddress("redis://${executionParameters.redisHost}:${executionParameters.redisPort}")
         .setTimeout(10000)

      try {
         Redisson.create(config).also {
            logger.info("Connection cache success")
         }
      } catch (e: Exception) {
         logger.error("Connection cache error", e)
         null
      }
   }
}

object CacheFactory {

   fun <T> createCache(db: RedisDb): RMapCache<String, T?>? {
      return Redis.client?.getMapCache<String, T>(db.toString())
   }
}
