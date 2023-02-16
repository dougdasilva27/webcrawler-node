package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.models.RankingProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils
import org.json.JSONObject
import java.net.URLDecoder


/**
 * Date: 22/07/20
 *
 * @author Fellype Layunne
 *
 */
class BrasilRennerCrawler(session: Session) : CrawlerRankingKeywords(session) {

   init {
      pageSize = 40
   }

   override fun extractProductsFromCurrentPage() {

      val json = requestApi()

      if (this.totalProducts == 0) {
         setTotalProducts(json)
      }

      val elements = json.optJSONArray("docs")

      if (elements.isEmpty) {
         result = false
         return
      }

      for (elem in elements) {
         val element = (elem as JSONObject)
         val internalId = element.optJSONArray("sku_list").optString(0)
         val internalPid = element.optString("parent_product_id")

         val productUrl = crawlProductUrl(element)
         val productName = element.optString("name")
         val productPrice = scrapPrice(element)
         val productImage = element.optString("imageId")
         val available = element.optString("inventory_status", "").equals("INSTOCK", true)

         val productRanking = RankingProductBuilder.create()
            .setUrl(productUrl)
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setImageUrl(productImage)
            .setName(productName)
            .setPriceInCents(productPrice)
            .setAvailability(available)
            .build()

         saveDataProduct(productRanking)
         if (arrayProducts.size == productsLimit) {
            break
         }
      }
   }

   private fun scrapPrice(element: JSONObject): Int? {
      if (element.has("salePriceCents")) {
         return element.optInt("salePriceCents")
      } else {
         return element.optInt("priceCents")
      }
   }

   private fun crawlProductUrl(element: JSONObject): String {
      return URLDecoder.decode(element.optString("clickUrl").substringAfter("&ct=").replace("&redirect=true", ""), "UTF-8");
   }

   private fun requestApi(): JSONObject {
      val url =
         "https://recs.richrelevance.com/rrserver/api/find/v1/24f07d816ef94d7f?query=$keywordEncoded" + "&placement=search_page.find&mm=5%3C90%25&sort=&start=${(currentPage - 1) * pageSize}" + "&rows=$pageSize" + "&log=true&ssl=true&lang=pt&rcs=eF5j4cotK8lMETA0NzfVNdQ1ZClN9kg2M09KMjBP1jUwMDPRNUk2StVNTgJxk80MLdKMkszNE1MBmHAOwA"

      val headers: MutableMap<String, String> = HashMap()
      headers["Content-Type"] = "application/json"

      val request = Request.RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).setProxyservice(listOf(ProxyCollection.LUMINATI_SERVER_BR, ProxyCollection.BUY))
         .mustSendContentEncoding(false).build()

      val json = CrawlerUtils.stringToJson(dataFetcher[session, request].body)

      return json.optJSONArray("placements").optJSONObject(0)
   }

   override fun hasNextPage(): Boolean {
      return true
   }

   fun setTotalProducts(search: JSONObject) {

      val total = search.opt("numFound")

      if (total is Int) {
         totalProducts = total
      }
   }
}

