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


@ExperimentalTime
object CrawlerCache {
   private val logger = LoggerFactory.getLogger(CrawlerCache::class.java)

   private val cache = RedisClient.client
   private const val logPrefix = "[Redis]"

   private val mapCache: RMapCache<String, Any?> = cache.getMapCache("Crawler")

   private const val defaultTtl = 7200

   fun <T> get(key: String, type: Class<T>): T? {
      val timedValue: TimedValue<Any?> = measureTimedValue { mapCache[key] }
      logger.debug("$logPrefix [Get] ${timedValue.duration.inMilliseconds} ms")
      val value = timedValue.value
      return if (value != null && type.isAssignableFrom(value::class.java)) {
         type.cast(value)
      } else null
   }

   fun <T> setExKey(key: String, value: T, seconds: Int) {
      val duration: Duration = measureTime { mapCache.put(key, value, seconds.toLong(), TimeUnit.SECONDS) }
      logger.debug("$logPrefix [Set] ${duration.inMilliseconds} ms")
   }

   fun <T> setKey(key: String, value: T) {
      setExKey(key, value, defaultTtl)
   }

   fun <T> getPutCache(
      key: String,
      ttl: Int,
      requestMethod: RequestMethod,
      request: Request,
      function: Function<Response, T>,
      dataFetcher: DataFetcher,
      session: Session
   ): T? {
      var value: Any? = get(key, Any::class.java)
      if (value == null) {
         value = when (requestMethod) {
            RequestMethod.GET -> function.apply(dataFetcher.get(session, request))
            RequestMethod.POST -> function.apply(dataFetcher.post(session, request))
            else -> throw RequestMethodNotFoundException(requestMethod.name)
         }
         setExKey(key, value as Any, ttl)
      }
      return value as T?
   }

   fun <T> getPutCache(key: String, requestMethod: RequestMethod, request: Request, function: Function<Response, T>, dataFetcher: DataFetcher, session: Session): T? {
      return getPutCache(key, defaultTtl, requestMethod, request, function, dataFetcher, session)
   }
}
