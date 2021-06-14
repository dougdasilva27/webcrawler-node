@file:Suppress("UNCHECKED_CAST")

package br.com.lett.crawlernode.integration.redis

import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.fetcher.models.Response
import br.com.lett.crawlernode.core.models.RequestMethod
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.exceptions.RequestMethodNotFoundException
import br.com.lett.crawlernode.integration.redis.config.RedisDb
import io.lettuce.core.api.sync.RedisCommands
import java.util.function.Function

/**
 * Interface to interact with Redis.
 *
 */
class Cache(db: RedisDb) {

   private val mapCache: RedisCommands<String, Any?>? by lazy {
      CacheFactory.createCache(Any::class.java, db)
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
         val resp = when (requestMethod) {
            RequestMethod.GET -> dataFetcher.get(session, request)
            RequestMethod.POST -> dataFetcher.post(session, request)
            else -> throw RequestMethodNotFoundException(requestMethod.name)
         }
         if (isSuccess(resp)) {
            value = function.apply(resp)
            put(key, value as Any, ttl)
         }
      }
      return value as T?
   }

   private fun isSuccess(resp: Response): Boolean {
      val firstChar = resp.lastStatusCode.toString()[0].toString()
      return firstChar in arrayOf("2", "3")
   }
}
