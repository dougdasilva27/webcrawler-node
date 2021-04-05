package br.com.lett.crawlernode.integration.redis

import br.com.lett.crawlernode.main.GlobalConfigurations.executionParameters
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config

/**
 * Singleton synchronous redis client. It is lazily evaluated, won't connect to Redis until being used.  org.redisson.Redisson is completely thread safe
 *
 * @see RedisClient
 * @see CrawlerCache
 *
 * @author Charles Fonseca
 */
object RedisClient {

   val client: RedissonClient by lazy {
      val config = Config()
      config.useSingleServer().address = "redis://${executionParameters.redisHost}:${executionParameters.redisPort}"
      Redisson.create(config)
   }
}
