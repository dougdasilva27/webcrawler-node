package br.com.lett.crawlernode.crawlers.ranking.keywords.portugal

import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class PortugalContinenteCrawler(session: Session?) : CrawlerRankingKeywords(session) {

   init {
      pageSize = 20
   }

  override fun extractProductsFromCurrentPage() {

    val request = RequestBuilder.create()
      .setUrl("https://www.continente.pt/stores/continente/_vti_bin/eCsfServices/SearchServices.svc/ExecuteSearch")
      .setCookies(cookies)
      .setHeaders(mapOf("Content-Type" to "application/json"))
      .setPayload(
        """{
        "request": {
          "Refiners": [
            {
              "Name": "originallistprice",
              "Value": ""
            },
            {
              "Name": "primaryparentcategoryname",
              "Value": ""
            },
            {
              "Name": "brand",
              "Value": ""
            }
          ],
          "SearchText": "$keywordEncoded",
          "SearchScope": "1",
          "CategoryId": "",
          "NumberOfItemsToReturn": "20",
          "CurrentPage": "$currentPage",
          "Sort": [],
          "PropertiesToReturn": [
            "ProductId"
          ],
          "SpecialCategoryKey": "",
          "ContentTypes": [
            "Product"
          ],
          "PreventProductsInBasket": "false"
        }
      }""".replace(" ", "").replace("\n", "")
      ) .setProxyservice(Arrays.asList(
          ProxyCollection.INFATICA_RESIDENTIAL_BR,
          ProxyCollection.NETNUT_RESIDENTIAL_ES
       )).build()

    val body = dataFetcher.post(session, request)?.body
    val jsonObject = CrawlerUtils.stringToJSONObject(body)
    val result = jsonObject?.optJSONObject("d")
    val resultItems = result?.optJSONArray("SearchResultItems") ?: JSONArray()
    for (array in resultItems) {

      if (array is JSONArray) {
        for (json in array) {

          if (json is JSONObject) {
            val propName = json.optString("Name")

            if (propName == "ProductId") {
              val internalId = json.optString("Value", null)?.substringBefore("(")
              val productUrl = "https://www.continente.pt/stores/continente/pt-pt/public/Pages/ProductDetail.aspx?ProductId=$internalId"
              log("internalId - $internalId - url $productUrl")
              saveDataProduct(internalId, null, productUrl)
              break
            }
          }
        }
      }
    }
    if (totalProducts == 0) {
      totalProducts = result?.optInt("TotalRows") ?: 0
    }
  }
}
