package br.com.lett.crawlernode.crawlers.corecontent.belgium

import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.models.CategoryCollection
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import exceptions.MalformedPricesException
import org.apache.http.impl.cookie.BasicClientCookie
import org.json.JSONObject
import org.jsoup.nodes.Document
import java.util.*
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder
import br.com.lett.crawlernode.test.Test

abstract class ColruytCrawler(session: Session) : Crawler(session) {

   /*
   * para pegar o placeId tem que entrar na home do site https://www.colruyt.be/fr
   * ir em Liste de courses no lado direito da tela, colocar o `code postal` ou o endereço
   * e escolher a unidade em Sélectionné e CONFIRMER.
   * Depois, escolher o produto.
   *
   * Apos isso, para pegar o placeId, abre a ferramenta network do navegador e procura pela requisição:
   * https://ecgproductmw.colruyt.be/ecgproductmw/v2/fr/products/0000?clientCode=clp&placeId=????
   *
   * */
   abstract fun getPlaceId(): String

     override fun fetch(): JSONObject? {
	   
	    val productId = CommonMethods.getLast(session.getOriginalURL().split("pid="));
	   
      val headers: MutableMap<String, String> = HashMap()
      headers["User-Agent"] = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.80 Safari/537.36"
      headers["Origin"] = "https://www.colruyt.be"

      val url = "https://ecgproductmw.colruyt.be/ecgproductmw/v2/fr/products/${productId}?clientCode=clp&placeId=${getPlaceId()}&dataGroup=ALL"

      val result = dataFetcher.get(
         session, RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(listOf(
            ProxyCollection.LUMINATI_SERVER_BR,
            ProxyCollection.BONANZA_BELGIUM,
            ProxyCollection.NETNUT_RESIDENTIAL_ES))
         .build()
      )?.body
	   
      return result?.toJson()
   }
	
	
   override fun extractInformation(jsonObject: JSONObject): MutableList<Product> {
      Logging.printLogInfo(logger, session, "Product page identified: " + session.originalURL)
      val products = mutableListOf<Product>()

       products += product {
          url = session.originalURL
          categories = scrapCategories(jsonObject)
          name = "${jsonObject.optString("brand")} ${jsonObject.optString("name")} ${jsonObject.optString("content")}".trim()
          description = jsonObject.optString("description")
          primaryImage = jsonObject.optString("fullImage")
          internalId = jsonObject.optString("commercialArticleNumber")
          internalPid = jsonObject.optString("productId")
  
          if (jsonObject.optBoolean("isAvailable")) {
             offer {
                isMainRetailer
                useSlugNameAsInternalSellerId
                sellerFullName = "Colruyt"
                pricing {
                   val price = jsonObject.optJSONObject("price")?.optDouble("basicPrice")
                   spotlightPrice = price
                   bankSlip = price?.toBankSlip() ?: throw MalformedPricesException()
                }
             }
          } else {
             offer {  }
          }
       }

      if (products.isEmpty()) {
         Logging.printLogInfo(logger, session, "not a product page: " + session.originalURL)
      }
      return products
   }

   fun scrapCategories(json: JSONObject): CategoryCollection {

      val categories = CategoryCollection()

      val jsonArray = JSONUtils.getJSONArrayValue(json, "categories")

      if (jsonArray.isEmpty) {
         return categories
      }
      var currentCategory: Any? = jsonArray[0]
      for (x in 0..2) {

         if (currentCategory == null) {
            break
         }

         if (currentCategory is JSONObject) {
            val name = currentCategory.optString("name")
            if(name != null) {
               categories.add(name)
               val children = currentCategory.optJSONArray("children")
               currentCategory = children?.get(0)
            } else {
               break
            }
         }
      }

      return categories
   }

   fun waitLoad(time: Int) {
      try {
         Thread.sleep(time.toLong())
      } catch (e: InterruptedException) {
         e.printStackTrace()
      }
   }
}
