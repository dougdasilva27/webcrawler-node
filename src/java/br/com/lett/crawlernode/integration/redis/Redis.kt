package br.com.lett.crawlernode.integration.redis

import br.com.lett.crawlernode.integration.redis.config.RedisDb
import br.com.lett.crawlernode.main.GlobalConfigurations.executionParameters
import org.redisson.Redisson
import org.redisson.api.RMapCache
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.redisson.jcache.configuration.RedissonConfiguration
import javax.cache.Cache
import javax.cache.CacheManager
import javax.cache.Caching
import javax.cache.configuration.MutableConfiguration


/**
 * Singleton synchronous redis client. It is lazily evaluated, won't connect to Redis until being used.
 *
 */
object Redis {

   internal val client: RedissonClient? by lazy {
      val config = Config()
      config.useSingleServer().setAddress("redis://${executionParameters.redisHost}:${executionParameters.redisPort}")
         .setTimeout(10000)

      try {
         Redisson.create(config)
      } catch (e: Exception) {
         null
      }
   }
}

object CacheFactory {

   fun <T> createCache(db: RedisDb): RMapCache<String, T?>? {
      return Redis.client?.getMapCache<String, T>(db.toString())
   }
}
