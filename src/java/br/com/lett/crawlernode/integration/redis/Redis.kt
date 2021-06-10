package br.com.lett.crawlernode.integration.redis

import br.com.lett.crawlernode.main.GlobalConfigurations.executionParameters
import com.google.gson.GsonBuilder
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.codec.RedisCodec
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.time.Duration


/**
 * Singleton synchronous redis client. It is lazily evaluated, won't connect to Redis until being used.
 *
 * @see Redis
 * @see Cache
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

   fun shutdown() {
      client?.shutdown()
   }
}

object CacheFactory {
   fun <T> createCache(): StatefulRedisConnection<String, T?>? {
      return Redis.client?.connect(JsonCodec<T>())
   }
}

enum class CacheType {
   CRAWLER, RANKING
}

class JsonCodec<T> : RedisCodec<String, T> {

   private val charset = Charset.forName("UTF-8")
   private val gson = GsonBuilder().create()

   override fun decodeKey(bytes: ByteBuffer?): String {
      return charset.decode(bytes).toString()
   }

   override fun decodeValue(bytes: ByteBuffer?): T {
      TODO("Not yet implemented")
   }

   override fun encodeKey(key: String): ByteBuffer {
      TODO("Not yet implemented")
   }

   override fun encodeValue(value: T): ByteBuffer {
      TODO("Not yet implemented")
   }

}
