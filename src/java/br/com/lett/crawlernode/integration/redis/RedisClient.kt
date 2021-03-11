package br.com.lett.crawlernode.integration.redis

import br.com.lett.crawlernode.main.GlobalConfigurations.executionParameters
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

object RedisClient {
   private val pool: JedisPool by lazy {
      JedisPool(JedisPoolConfig(), executionParameters.redisHost, executionParameters.redisPort)
   }

   fun getKey(key: String): String? = pool.resource.get(key)

   fun setExKey(key: String, value: String, seconds: Int): String? = pool.resource.setex(key, seconds, value)

   fun setKey(key: String, value: String): String? = pool.resource.set(key, value)
}
