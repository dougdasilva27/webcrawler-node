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
object CrawlerCache {

   private val mapCache by lazy { RedisClient.crawlerCache }

   /**
    * TTL (time to live)
    */
   private const val defaultTtl = 7200

   fun <T> get(key: String): T? {
      val value: Any? = mapCache[key]
      return if (value != null) value as T? else null
   }

   fun <T> put(key: String, value: T, seconds: Int) {
      mapCache.put(key, value, seconds.toLong(), TimeUnit.SECONDS)
   }

   fun <T> put(key: String, value: T) {
      put(key, value, defaultTtl)
   }

   @JvmOverloads
   fun <T> getPutCache(
      key: String,
      ttl: Int = defaultTtl,
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
}
