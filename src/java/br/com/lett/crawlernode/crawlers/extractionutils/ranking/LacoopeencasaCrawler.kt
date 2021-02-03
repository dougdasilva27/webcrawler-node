package br.com.lett.crawlernode.crawlers.extractionutils.ranking

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.JSONUtils
import br.com.lett.crawlernode.util.toJson
import org.json.JSONObject

/**
 * Date: 25/01/21
 *
 * @author Fellype Layunne
 *
 */
abstract class LacoopeencasaCrawler (session: Session) : CrawlerRankingKeywords(session){


   abstract fun getLocalId(): String

   private var nextProductCount = 0

   init {
      super.fetchMode = FetchMode.FETCHER
   }

   public override fun extractProductsFromCurrentPage() {
      pageSize = 0

      val search = fetchProductsFromAPI()

      val data = JSONUtils.getJSONValue(search, "datos")

      if (this.totalProducts == 0) {
         this.totalProducts = data.optInt("cantidad_articulos", 0)
         nextProductCount = 0
      }

      val products = JSONUtils.getJSONArrayValue(data, "articulos")

      nextProductCount += products.length()

      for (product in products) {

         if (product is JSONObject) {
            val internalId = product.optString("cod_interno", null)

            val productUrl = "https://www.lacoopeencasa.coop/producto" +
               "/${product.optString("descripcion").toLowerCase().replace(" ", "-")}" +
               "/${internalId}"

            saveDataProduct(internalId, null, productUrl)
            log("Position: $position - InternalId: $internalId - InternalPid: null - Url: $productUrl")
            if (arrayProducts.size == productsLimit) {
               break
            }
         }
      }

      log("Finalizando Crawler de produtos da página $currentPage - até agora ${arrayProducts.size} +  produtos crawleados")
   }

   override fun processBeforeFetch() {
      val referer = "https://www.lacoopeencasa.coop/listado/busqueda-avanzada/${getKeyword()}"
      cookies = br.com.lett.crawlernode.crawlers.extractionutils.core.LacoopeencasaCrawler.getCookies(dataFetcher, session, getLocalId(), referer)
   }

   private fun fetchProductsFromAPI(): JSONObject {
      val url = "https://www.lacoopeencasa.coop/ws/index.php/categoria/categoriaController/filtros_busqueda"

      val referer = "https://www.lacoopeencasa.coop/listado/busqueda-avanzada/${getKeyword().toLowerCase()}"

      val headers = getHeaders(referer)
      headers["Content-Type"] = "application/json"
      headers["Cookie"] += this.cookies.firstOrNull { it.name == "_lcec_linf" }?.let { "${it.name}=${it.value};" } ?: ""

      val payload = "{\"pagina\":0,\"filtros\":{\"preciomenor\":-1,\"preciomayor\":-1,\"categoria\":[],\"marca\":[],\"tipo_seleccion\":\"busqueda\",\"tipo_relacion\":\"busqueda\",\"filtros_gramaje\":[]," +
         "\"termino\":\"${getKeyword()}\"," +
         "\"cant_articulos\":${nextProductCount}," +
         "\"ofertas\":false,\"modificado\":false,\"primer_filtro\":\"\"}}"

      val request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .build()

      val response = dataFetcher.post(session, request)

      return response.body.toJson()
   }

   private fun getKeyword(): String {
      return keywordWithoutAccents.toUpperCase().replace(" ", "_")
   }

   private fun getHeaders(referer: String): MutableMap<String, String> {
      return br.com.lett.crawlernode.crawlers.extractionutils.core.LacoopeencasaCrawler.getHeaders(referer)
   }
}
