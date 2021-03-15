package br.com.lett.crawlernode.integration.redis

import br.com.lett.crawlernode.main.GlobalConfigurations.executionParameters
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config


object RedisClient {

   val client: RedissonClient by lazy {
      val config = Config()
      config.useSingleServer().address = "redis://${executionParameters.redisHost}:${executionParameters.redisPort}"
      Redisson.create(config)
   }
}
