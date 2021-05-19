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
      fetchMode = FetchMode.JAVANET
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
           "query": "query searchProducts(  ${"$"}query: String!  ${"$"}first: Int  ${"$"}offset: Int  ${"$"}orderOptions: String  ${"$"}filter: ProductFilterInput) {  search(query: ${"$"}query) {    products(first: ${"$"}first, offset: ${"$"}offset, orderOptions: ${"$"}orderOptions, filter: ${"$"}filter) {      edges {        node {          id          name          isAvailable          url          action          sku          category{            id            name          }          maxQuantityAllowed          useWeightPicker          availability{            lineMaturationOptions            quantityOnCheckout            variantOnCheckout            priceRange{              start {                gross {                  amount                }                }                stop {                gross {                  amount                }              }            }            priceRangeUndiscounted{              start {                gross {                  amount                }              }              stop {                gross {                  amount                }              }            }          }          thumbnail{              url          }          price{              amount              currency          }          variants{              id              name              stockQuantity              weightUnit              isPiece              maturationOptions          }          shoppingList{              id              name          }        }      }    }    pages    total  }}",
           "variables": {
             "query": "$location",
             "first": $pageSize,
             "offset": $offset,
             "orderOptions": "name",
             "filter": {
               "postalCode": "$POSTAL_CODE"
             }
           },
           "operationName": null
         }""".trimIndent()
      val headers = mapOf("Content-Type" to "application/json")

      val request = Request.RequestBuilder.create().setUrl(url).setCookies(cookies)
         .setPayload(body).setHeaders(headers).build()

      val response = dataFetcher.post(session, request)
      val json = CrawlerUtils.stringToJson(response.body)
      val apiResp = json.optQuery("/data/search") as JSONObject

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
