package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilSitemercadoCrawler
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.JSONUtils
import org.apache.http.HttpHeaders
import org.json.JSONObject
import java.net.URI

class BrasilNagumoCrawler(session: Session) : BrasilSitemercadoCrawler(session) {

   companion object {
      private const val HOME_PAGE = "https://www.nagumo.com.br/guarulhos-lj42-guarulhos-aruja-jardim-cumbica-caminho-do-campo-do-rincao"
      private const val API_URL = "https://b2c-api-premiumlabel-production.azurewebsites.net/api/"

      private const val IDLOJA = 4951
      private const val IDREDE = 884
   }

   override fun getHomePage(): String {
      return HOME_PAGE
   }

   override fun getLojaInfo(): Map<String, Int> {
      val lojaInfo: MutableMap<String, Int> = HashMap()
      lojaInfo["IdLoja"] = IDLOJA
      lojaInfo["IdRede"] = IDREDE
      return lojaInfo
   }

   override fun getApiUrl(): String {
      return API_URL
   }

   override fun getLoadApiUrl(): String {
      return API_URL
   }

   override fun crawlProductInformatioFromApi(productUrl: String): JSONObject? {
      val lojaUrl = URI.create(session.originalURL).path.split("/").first(String::isNotEmpty)
      val loadUrl = "${API_URL}v1/b2c/page/store/$lojaUrl"
      val url = "${API_URL}b2c/product/${productUrl.split("/").last().split("\\?").first()}?store_id=${lojaInfo["IdLoja"]}"
      val headers: MutableMap<String, String?> = mutableMapOf(
         HttpHeaders.REFERER to productUrl,
         HttpHeaders.ACCEPT to "application/json, text/plain, */*",
         HttpHeaders.CONTENT_TYPE to "application/json",
         HttpHeaders.USER_AGENT to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36"
      )
      val request = Request.RequestBuilder.create().setUrl(loadUrl).setCookies(cookies).setHeaders(headers).setPayload(loadPayload).build()
      val responseHeaders = ApacheDataFetcher()[session, request].headers

      val jsonObject = if (responseHeaders != null) JSONUtils.stringToJson(responseHeaders["sm-token"]) else JSONObject()
      jsonObject.put("IdLoja", lojaInfo["IdLoja"])
         .put("IdRede", lojaInfo["IdRede"])
      headers["sm-token"] = jsonObject.toString()
      headers["sm-mmc"] = responseHeaders["sm-mmc"]
      val requestApi = Request.RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).build()
      return CrawlerUtils.stringToJson(dataFetcher[session, requestApi].body)
   }

}
