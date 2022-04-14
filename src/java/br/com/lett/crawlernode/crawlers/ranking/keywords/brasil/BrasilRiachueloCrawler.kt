package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.models.RankingProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.toJson
import org.json.JSONObject

class BrasilRiachueloCrawler(session: Session) : CrawlerRankingKeywords(session) {

   init {
      pageSize = 24
      fetchMode = FetchMode.FETCHER
   }

   fun getToken(apiKey: String): String? {
      val urlToken = "https://9hyxh9dsj1.execute-api.us-east-1.amazonaws.com/v1/bf60cb91-a86d-4a68-86eb-46855b4738c8/get-token"

      val headers = mapOf(
         "accept" to "*/*",
//         "connection" to "keep-alive",
         "x-api-key" to apiKey
      )

      val payload = JSONObject()
      payload.put("value", "SjqJQstBFWjIqzYzP73umkNHT7RTeWcHanVu1K7mGYHrqIskym+BvChLueA0qnAstBZzgVcwOt/UNlU1wXbhJ7ta6/8esxROylJS6kTk3VEw1l3QBHijzGk/CF8afz1HmOHFFQ4u/+N7+GqJ1Pax8BmrOt3KitkBF47zyxMagTAUruSogIx0A/ib7JtSUvDHLi53MRlODpjG/Pezkm/EhhczAjYk2+3bRWMu0/nk3KknXXoO+SDf826ukLDpkfjwg8OUYOTWdvt5X7WiuspIB2E5ZklYYK8C8hxda3Sy5QaGngElEgzZfZkcC0slJuVMSS3+7F6ysxgKLIX0K1LZPZALGe7BtEsCKMDv9L2LarGUzZkOJT9X6kFa3wsQj3YggZtIGASIznkWWUg0hhrX+FzWsvjwhxvCaX4LYpXQ2byA9lmlliZ1wtf0ZvNmrjc01tzvZHfm67PdqO3VHqK+tEhlVdTZuQlWb4ekExpkyoKpZnkqSVdpQ/LkemnKgzVmah00EvCWOJhFgEzqxxTCRobzBoUKNmj/ZSg51H/3e95+Xxdpf0Y5+TIpuWyq79tY3ZxQcUceF0dQUQlptRIlOjzt9jGHyYrO5El3PwAH1FOvyQialAomF2mjo2ffa73l9d6IN+8H+6s5dVUYsT9FCqeO1RKveZcWQ5TEVe+Y5lw=")

      val request = Request.RequestBuilder.create()
         .setUrl(urlToken)
         .setIgnoreStatusCode(false)
         .mustSendContentEncoding(false)
         .setHeaders(headers)
         .setPayload(payload.toString())
         .setFetcheroptions(
            FetcherOptionsBuilder.create()
               .mustUseMovingAverage(false)
               .mustRetrieveStatistics(true)
               .build()
         ).build()

      return dataFetcher.post(session, request)?.body?.toJson()?.optString("IdToken")
   }

   fun fetchJSONObject(url: String, token: String, apiKey: String): JSONObject? {
//      val payload = JSONObject()
//      payload.put("attributes", "{}")
//      payload.put("includeFilters", "true")
//      payload.put("order", "asc")
//      payload.put("page", "1")
//      payload.put("price", "[]")
//      payload.put("q", this.keywordEncoded)
//      payload.put("soldOut", "false")
//      payload.put("sort", "relevance")

      val payload = "{\"includeFilters\":true,\"order\":\"asc\",\"q\":\"" + this.keywordEncoded + "\",\"sort\":\"relevance\",\"attributes\":{},\"price\":[],\"page\":" + this.currentPage + ",\"soldOut\":false}";

      val headers = mapOf(
         "Accept" to "*/*",
         "Content-type" to "application/json",
         "Content-Length" to payload.toString().length.toString(),
         "Host" to "api-dc-rchlo-prd.riachuelo.com.br",
         "Origin" to "https://www.riachuelo.com.br",
         "Referer" to "https://www.riachuelo.com.br/",
         "accept-encoding" to "no",
         "Connection" to "keep-alive",
         "x-api-key" to apiKey,
         "x-app-token" to token,
         "x-ciano" to "true"
      )


      val request = Request.RequestBuilder.create()
         .setUrl(url)
         .setIgnoreStatusCode(false)
         .mustSendContentEncoding(false)
         .setHeaders(headers)
         .setPayload(payload.toString())
         .setTimeout(20000)
         .setFetcheroptions(
            FetcherOptionsBuilder.create()
               .mustUseMovingAverage(false)
               .mustRetrieveStatistics(true)
               .build()
         )
         .build()

      var resp = dataFetcher.post(session, request)
      var content = resp?.body?.toJson()

      var tries = 0
      if (content != null) {
         while (content?.isEmpty == true && tries < 3) {
            resp = dataFetcher.post(session, request)
            content = resp?.body?.toJson()
            tries++
         }
      }

      return content
   }

   override fun extractProductsFromCurrentPage() {

      val url = "https://api-dc-rchlo-prd.riachuelo.com.br/ecommerce-web-catalog/v2/products"
      val apiKey = "KhMO3jH1hsjvSRzQXNfForv5FrnfSpX6StdqMmjncjGivPBj3MS4kFzRWn2j7MPn"

      val token = getToken(apiKey)
      val jsonApi = token?.let { fetchJSONObject(url, it, apiKey) }
      val total = jsonApi?.optInt("total")
      if (totalProducts == 0) {
         totalProducts = total?: 0
      }
      val jsonArray = jsonApi?.optJSONArray("products")

      if (jsonArray != null && !jsonArray.isEmpty()) {
         for (product in jsonArray) {
            if (product is JSONObject) {
               val productUrl = "https://www.riachuelo.com.br/" + product.optString("url")
               val internalId = product.opt("sku")
               val internalPid = product.opt("sku")
               val imgUrl = product.optQuery("/images/0")
               val name = product.optString("name")
               val price = product.optJSONObject("price").optDouble("final")
               val priceInCents = (price * 100).toInt()
               var isAvailable = false
               isAvailable = price > 0

               val productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId as String?)
                  .setInternalPid(internalPid as String?)
                  .setImageUrl(imgUrl as String?)
                  .setName(name)
                  .setPriceInCents(priceInCents)
                  .setAvailability(isAvailable)
                  .build()

               saveDataProduct(productRanking)

               if (arrayProducts.size == productsLimit) {
                  break
               }
            }
         }
      } else {
         this.result = false
         this.log("Keyword sem resultado!")
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size + " produtos crawleados")

   }

}
