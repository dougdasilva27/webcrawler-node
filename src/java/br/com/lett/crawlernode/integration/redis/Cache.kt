@file:Suppress("UNCHECKED_CAST")

package br.com.lett.crawlernode.integration.redis

import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.fetcher.models.Response
import br.com.lett.crawlernode.core.models.RequestMethod
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.exceptions.RequestMethodNotFoundException
import io.lettuce.core.api.sync.RedisCommands
import java.util.function.Function

/**
 * Interface to interact with Redis.
 *
 */
class Cache {

   private val mapCache: RedisCommands<String, Any?>? by lazy {
      CacheFactory.createCache()
   }

   private val defaultTimeSecs: Long = 7200

   fun <T> get(key: String): T? {
      val value: Any? = mapCache?.get(key)
      return if (value != null) value as T? else null
   }

   fun <T> put(key: String, value: T, seconds: Long) {
      mapCache?.setex(key, seconds, value)
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
   fun <T> getPutCache(
      key: String,
      ttl: Long = defaultTimeSecs,
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
