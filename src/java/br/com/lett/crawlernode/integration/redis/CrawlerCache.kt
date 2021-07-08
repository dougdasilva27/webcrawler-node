@file:Suppress("UNCHECKED_CAST")

package br.com.lett.crawlernode.integration.redis

import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.fetcher.models.Response
import br.com.lett.crawlernode.core.models.RequestMethod
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.exceptions.RequestMethodNotFoundException
import br.com.lett.crawlernode.integration.redis.config.RedisDb
import org.redisson.api.RMapCache
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import java.util.function.Function

/**
 * Interface to interact with Redis.
 *
 */
class CrawlerCache(db: RedisDb) {

   private val logger = LoggerFactory.getLogger(CrawlerCache::class.java)

   private val mapCache: RMapCache<String, Any?>? by lazy {
      CacheFactory.createCache(db)
   }

   private val defaultTimeSecs: Long = 7200

   fun <T> get(key: String): T? {
      val started = System.currentTimeMillis()
      val value: Any? = mapCache?.get(key)
      logger.debug("[Redis timing] Get {}", System.currentTimeMillis() - started)
      return if (value != null) value as T? else null
   }

   fun <T> set(key: String, value: T, seconds: Long) {
      val started = System.currentTimeMillis()
      mapCache?.fastPut(key, value, seconds, TimeUnit.SECONDS)
      logger.debug("[Redis timing] Put {}", System.currentTimeMillis() - started)
   }

   /**
    * @param key cache key
    * @param ttl time to live in seconds
    * @param requestMethod request method e.g. GET, POST
    * @param request request being cached
    * @param function should return object to be cached
    * @param dataFetcher http client
    * @param session session data
    */
   @JvmOverloads
   fun <T> update(
      key: String,
      request: Request,
      requestMethod: RequestMethod,
      session: Session,
      dataFetcher: DataFetcher,
      mustUpdate: Boolean = false,
      ttl: Long = defaultTimeSecs,
      function: Function<Response, T>
   ): T? {
      var value: Any? = null
      if (!mustUpdate) {
         value = get(key)
      }
      if (value == null) {
         val resp = when (requestMethod) {
            RequestMethod.GET -> dataFetcher.get(session, request)
            RequestMethod.POST -> dataFetcher.post(session, request)
            else -> throw RequestMethodNotFoundException(requestMethod.name)
         }
         if (resp.isSuccess) {
            value = function.apply(resp)
            set(key, value as Any, ttl)
         }
      }
      return value as T?
   }
}
