@file:Suppress("UNCHECKED_CAST")

package br.com.lett.crawlernode.integration.redis

import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.fetcher.models.Response
import br.com.lett.crawlernode.core.models.RequestMethod
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.exceptions.RequestMethodNotFoundException
import org.redisson.api.RMapCache
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import java.util.function.Function
import kotlin.time.*

/**
 * Interface to interact with Redis.
 *
 * @see RedisClient
 * @see RMapCache
 * @author Charles Fonseca
 */
@ExperimentalTime
object CrawlerCache {
   private val logger = LoggerFactory.getLogger(CrawlerCache::class.java)

   private val cache = RedisClient.client
   private const val logPrefix = "[Redis]"

   /**
    * Interface we interact with when it comes to get/put on cache
    */
   private val mapCache: RMapCache<String, Any?> = cache.getMapCache("Crawler")

   /**
    * TTL (time to live)
    */
   private const val defaultTtl = 7200

   fun <T> get(key: String): T? {
      val timedValue: TimedValue<Any?> = measureTimedValue { mapCache[key] }
      logger.debug("$logPrefix [Get] ${timedValue.duration.inMilliseconds} ms")
      val value = timedValue.value
      return if (value != null) value as T? else null
   }

   fun <T> put(key: String, value: T, seconds: Int) {
      val duration: Duration = measureTime { mapCache.put(key, value, seconds.toLong(), TimeUnit.SECONDS) }
      logger.debug("$logPrefix [Set] ${duration.inMilliseconds} ms")
   }

   fun <T> put(key: String, value: T) {
      put(key, value, defaultTtl)
   }

   @JvmOverloads
   fun <T> getPutCache(
      key: String,
      ttl: Int = 7200,
      requestMethod: RequestMethod,
      request: Request,
      function: Function<Response, T>,
      dataFetcher: DataFetcher,
      session: Session
   ): T? {
      var value: Any? = get(key)
      if (value == null) {
         value = when (requestMethod) {
            RequestMethod.GET -> function.apply(dataFetcher.get(session, request))
            RequestMethod.POST -> function.apply(dataFetcher.post(session, request))
            else -> throw RequestMethodNotFoundException(requestMethod.name)
         }
         put(key, value as Any, ttl)
      }
      return value as T?
   }

   fun shutdown() {
      cache.shutdown()
   }
}
