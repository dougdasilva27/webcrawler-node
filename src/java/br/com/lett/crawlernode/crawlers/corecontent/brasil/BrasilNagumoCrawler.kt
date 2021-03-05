package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.corecontent.araraquara.AraraquaraSitemercadosupermercados14Crawler
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilSitemercadoCrawler
import br.com.lett.crawlernode.util.CommonMethods
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.JSONUtils
import org.apache.http.HttpHeaders
import org.json.JSONObject

class BrasilNagumoCrawler(session: Session) : BrasilSitemercadoCrawler(session) {

    companion object {
        private const val HOME_PAGE = "https://www.nagumo.com.br/guarulhos-lj42-guarulhos-aruja-jardim-cumbica-caminho-do-campo-do-rincao"
        private const val API_URL = "https://b2c-api-premiumlabel.sitemercado.com.br/api/v1/b2c/"

        private const val IDLOJA = 2700
        private const val IDREDE = 884
    }

    override fun getHomePage(): String {
        return HOME_PAGE
    }
   
    override fun getLojaInfo(): Map<String, Int>? {
        val lojaInfo: MutableMap<String, Int> = HashMap()
        lojaInfo["IdLoja"] = IDLOJA
        lojaInfo["IdRede"] = IDREDE
        return lojaInfo
    }

   override fun getApiUrl(): String {
      return API_URL
   }

   override fun crawlProductInformatioFromApi(productUrl: String): JSONObject? {
      val lojaUrl = CommonMethods.getLast(homePage.split("www.nagumo.com.br").toTypedArray())
      val loadUrl = API_URL + "page/store" + lojaUrl
      val url = API_URL + lojaInfo!!["IdLoja"] + "/product/" + CommonMethods.getLast(productUrl.split("/").toTypedArray()).split("\\?").toTypedArray()[0]
      val headers: MutableMap<String, String?> = HashMap()
      headers[HttpHeaders.REFERER] = productUrl
      headers[HttpHeaders.ACCEPT] = "application/json, text/plain, */*"
      headers[HttpHeaders.CONTENT_TYPE] = "application/json"
      headers[HttpHeaders.USER_AGENT] = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36"
      val request = Request.RequestBuilder.create().setUrl(loadUrl).setCookies(cookies).setHeaders(headers).setPayload(loadPayload).build()
      val responseHeaders = ApacheDataFetcher()[session, request].headers
      val jsonObject = if (responseHeaders != null) JSONUtils.stringToJson(responseHeaders["sm-token"]) else JSONObject()
      // jsonObject.remove("IdLoja");
      // jsonObject.remove("IdRede");
      jsonObject.put("IdLoja", lojaInfo!!["IdLoja"])
      jsonObject.put("IdRede", lojaInfo!!["IdRede"])
      headers["sm-token"] = jsonObject.toString()
      headers["sm-mmc"] = responseHeaders!!["sm-mmc"]
      headers[HttpHeaders.ACCEPT_LANGUAGE] = "en-US,en;q=0.9,pt-BR;q=0.8,pt;q=0.7"
      val requestApi = Request.RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).build()
      return CrawlerUtils.stringToJson(dataFetcher[session, requestApi].body)
   }


}
