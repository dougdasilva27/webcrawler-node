package br.com.lett.crawlernode.crawlers.corecontent.belgium

import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import exceptions.MalformedPricesException
import org.apache.http.impl.cookie.BasicClientCookie
import org.json.JSONObject
import java.util.*

class BelgiumColruytCrawler(session: Session) : Crawler(session) {

   override fun handleCookiesBeforeFetch() {
      Logging.printLogDebug(logger, session, "Adding cookie...")
      cookies = CrawlerUtils.fetchCookiesFromAPage("https://www.colruyt.be/fr/produits", Arrays.asList("ASP.NET_SessionId"), "eproductmw.colruyt.be", "/",
         cookies, session, HashMap(), dataFetcher)
      val cookie = BasicClientCookie("noLocalizar", "true")
      cookie.domain = "www.disco.com.ar"
      cookie.path = "/"
      cookies.add(cookie)
   }

   override fun fetch(): Any {
      val headers: MutableMap<String, String> = HashMap()
      headers["Cookie"] = "DG_ZUID=E071E6EC-9AC1-3BDD-96DD-78EFAB377B7E; DG_HID=7B9A156F-E448-3827-83FA-604B4DD98430; DG_SID=51.254.117.237:wP1NKH8WH7lH974t55znO88ezakZEZ5OZTa6WpXhceQ; DG_IID=C5788001-A86F-3DE7-811E-0B6E65502FA2; DG_UID=A41A8DDD-0FA4-3E98-BF02-2A9B1315F27C;"

      val id = session.originalURL?.split("/")?.last()?.split("-")?.last() ?: throw IllegalStateException("Token must not be null")
      val url = "https://eproductmw.colruyt.be/eproductmw/rest/v1/fr/stores/3515/product-by-id/$id.json"
      val result = dataFetcher.get(
         session, RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(listOf(
            ProxyCollection.BE_OXYLABS,
            ProxyCollection.STORM_RESIDENTIAL_US,
            ProxyCollection.NETNUT_RESIDENTIAL_ES))
         .build()
      )?.body
      return result?.toJson() ?: throw IllegalStateException("It was not possible to fetch $url")
   }

   override fun extractInformation(json: JSONObject): MutableList<Product> {
      val products = mutableListOf<Product>()
      val data = json.optJSONObject("data")
      data?.let { jsonObject ->
         products += product {
            url = session.originalURL
            categories = jsonObject.optString("topCategoryName").split("/")
            name = jsonObject.optString("seoArticleName")
            description = jsonObject.optString("fullDescription")
            primaryImage = jsonObject.optString("detailImage")
            internalId = jsonObject.optString("commercialId")
            internalPid = internalId
            offer {
               isMainRetailer
               useSlugNameAsInternalSellerId
               sellerFullName = "Colruyt"
               pricing {
                  val price = jsonObject.optString("price1").toDoubleComma()
                  spotlightPrice = price
                  bankSlip = price?.toBankSlip() ?: throw MalformedPricesException()
               }
            }
         }
      }
      return products
   }
}
