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

   override fun fetch(): Document {
      dataFetcher = FetcherDataFetcher();
	   
      val headers: MutableMap<String, String> = HashMap()

      headers["Upgrade-Insecure-Requests"] = "1"
      headers["Accept-Language"] = "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7"
      headers["Sec-Fetch-Site"] = "same-origin"
      headers["Sec-Fetch-Mode"] = "navigate"
      headers["Sec-Fetch-User"] = "?1"
      headers["Sec-Fetch-Dest"] = "document"
      headers["Cache-Control"] = "max-age=0"
      headers["Connection"] = "keep-alive"
      headers["Accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"

      val result = FetcherDataFetcher().get(
         session, RequestBuilder.create()
         .setUrl(session.originalURL)
         .setHeaders(headers)
         .setCookies(cookies)
         .setFetcheroptions(
					  FetcherOptionsBuilder.create()
				        .setRequiredCssSelector("div[data-vue=productDetail]")
							  .mustRetrieveStatistics(true)
				        .build()
				  )
         .setProxyservice(listOf(
            ProxyCollection.INFATICA_RESIDENTIAL_BR,
            ProxyCollection.BONANZA_BELGIUM,
            ProxyCollection.NETNUT_RESIDENTIAL_ES))
         .build()
      )?.body
	   
      return result?.toDoc()?: throw IllegalStateException("It was not possible to fetch")
   }

   fun fetchProduct(productId: String, placeId: String): JSONObject? {

      if (productId.isEmpty() || placeId.isEmpty()) {
         return null
      }

      val headers: MutableMap<String, String> = HashMap()
      headers["User-Agent"] = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.80 Safari/537.36"
      headers["Origin"] = "https://www.colruyt.be"

      val url = "https://ecgproductmw.colruyt.be/ecgproductmw/v2/fr/products/${productId}?clientCode=clp&placeId=${placeId}&dataGroup=ALL"

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

   /*
   * Esse site tem um problema que quando você entra na página de algum produto,
   * as vezes não retorna nada. Por isso fiz uma essa função para tentar mais 3 vezes
   * até conseguir vim a página corretamente.
   * Caso mesmo assim não venha, o crawler retorna void
   * */
   fun scrapProductId(doc: Document): String {
      var currentDoc = doc
      val url = currentDoc.selectFirst("div[data-vue=productDetail]")?.attr("data-model-url")
  
      val productId = url?.substringAfter("model.")?.substringBefore(".json") ?: ""
  
      if (productId.isNotEmpty() && productId != "json") {
         return productId
      }
	   
      return ""
   }

   override fun extractInformation(doc: Document): MutableList<Product> {
      Logging.printLogInfo(logger, session, "Product page identified: " + session.originalURL)
      val products = mutableListOf<Product>()

      val productId = scrapProductId(doc)

      val data = fetchProduct(productId, getPlaceId())

      data?.let { jsonObject ->
         products += product {
            url = session.originalURL
            categories = scrapCategories(jsonObject)
            name = "${jsonObject.optString("brand")} ${jsonObject.optString("name")}".trim()
            description = jsonObject.optString("description")
            primaryImage = jsonObject.optString("fullImage")
            internalId = jsonObject.optString("commercialArticleNumber")
            internalPid = internalId

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
