package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils
import org.apache.http.impl.cookie.BasicClientCookie
import org.json.JSONArray
import org.json.JSONObject

class MexicoJustoCrawler(session: Session?) : CrawlerRankingKeywords(session) {

   companion object {
      private const val HOME_PAGE = "https://justo.mx"
      private const val POSTAL_CODE = "14300"
   }

   init {
      fetchMode = FetchMode.FETCHER
      pageSize = 25
   }


   public override fun processBeforeFetch() {
      val cookie = BasicClientCookie("postal_code", POSTAL_CODE)
      cookies.add(cookie)
   }

   override fun extractProductsFromCurrentPage() {
      val url = "$HOME_PAGE/graphql/"
      val offset = if (currentPage == 1) null else (currentPage - 1) * pageSize
      val body = """
         {
           "query": "query searchProducts(\n  ${'$'}query: String!\n  ${'$'}first: Int\n  ${'$'}offset: Int\n  ${'$'}orderOptions: String\n  ${'$'}filter: ProductFilterInput\n) {\n  search(query: ${'$'}query) {\n    products(first: ${'$'}first, offset: ${'$'}offset, orderOptions: ${'$'}orderOptions, filter: ${'$'}filter) {\n      edges {\n        node {\n          id\n          name\n          isAvailable\n          url\n          action\n          sku\n          category{\n            id\n            name\n          }\n          maxQuantityAllowed\n          useWeightPicker\n          availability{\n            lineMaturationOptions\n            quantityOnCheckout\n            variantOnCheckout\n            priceRange{\n              start {\n                gross {\n                  amount\n                }\n                }\n                stop {\n                gross {\n                  amount\n                }\n              }\n            }\n            priceRangeUndiscounted{\n              start {\n                gross {\n                  amount\n                }\n              }\n              stop {\n                gross {\n                  amount\n                }\n              }\n            }\n          }\n          thumbnail{\n              url\n          }\n          price{\n              amount\n              currency\n          }\n          variants{\n              id\n              name\n              stockQuantity\n              weightUnit\n              isPiece\n              maturationOptions {\n                description\n                name\n                type\n              }\n          }\n          shoppingList{\n              id\n              name\n          }\n        }\n      }\n    }\n    pages\n    total\n  }\n}",
           "variables": {
             "query": "$location",
             "first": $pageSize,
             "offset": $offset,
             "filter": {
               "postalCode": "$POSTAL_CODE"
             }
           },
           "operationName": null
         }
         """.trimIndent()
      val headers = mutableMapOf("Content-Type" to "application/json")

      val request = Request.RequestBuilder.create().setUrl(url).setCookies(cookies)
         .mustSendContentEncoding(false)
         .setPayload(body).setHeaders(headers).build()

      val response = dataFetcher.post(session, request)
      val json = CrawlerUtils.stringToJson(response.body)
      val apiResp = json.optQuery("/data/search") as JSONObject? ?: throw IllegalStateException("Not possible to retrieve searched data")

      if (currentPage == 1) {
         totalProducts = apiResp.optInt("total")
      }
      val products = apiResp.optQuery("/products/edges") as JSONArray? ?: throw IllegalStateException("Not possible to retrieve products' data")
      for (obj in products) {
         val product = (obj as JSONObject?)?.optJSONObject("node")
         val urlPath = product?.optString("url")
         val internalId = urlPath?.split("-")?.last()?.substringBeforeLast("/")
         val productUrl = HOME_PAGE + urlPath
         saveDataProduct(internalId, null, productUrl)
         log("Position: $position - InternalId: $internalId - Url: $productUrl")
      }
   }
}
