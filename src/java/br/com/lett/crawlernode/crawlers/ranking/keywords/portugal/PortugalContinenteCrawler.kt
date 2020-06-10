package br.com.lett.crawlernode.crawlers.ranking.keywords.portugal

import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils
import org.json.JSONArray
import org.json.JSONObject

class PortugalContinenteCrawler(session: Session?) : CrawlerRankingKeywords(session) {

  init {
    pageSize = 20
  }

  override fun extractProductsFromCurrentPage() {

    val request = RequestBuilder.create()
      .setUrl("https://www.continente.pt/stores/continente/_vti_bin/eCsfServices/SearchServices.svc/ExecuteSearch")
      .setPayload("""{
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
          "SearchText": "$keywordWithoutAccents",
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
      }""".replace("\t", "").replace("\n", "")
      ).build()
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
              val productUrl = "https://www.continente.pt/stores/continente/pt-pt/public/Pages/ProductDetail.aspx?ProductId=$propName"
              saveDataProduct(propName, null, productUrl)
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