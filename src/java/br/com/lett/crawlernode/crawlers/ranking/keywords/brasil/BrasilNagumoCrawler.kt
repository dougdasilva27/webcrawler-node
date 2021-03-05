package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler
import br.com.lett.crawlernode.util.CommonMethods
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.JSONUtils
import org.json.JSONObject

class BrasilNagumoCrawler(session: Session) : BrasilSitemercadoCrawler(session) {
   companion object {
      private const val HOME_PAGE = "https://www.nagumo.com.br/guarulhos-lj42-guarulhos-aruja-jardim-cumbica-caminho-do-campo-do-rincao"
      private const val API_URL = "https://b2c-api-premiumlabel.sitemercado.com.br/api/v1/b2c/"
   }

   override fun getHomePage(): String {
      return HOME_PAGE
   }

//    override fun getLoadPayload(): String {
//        val payload = JSONObject()
//        val split = HOME_PAGE.split("/".toRegex()).toTypedArray()
//        payload.put("lojaUrl", CommonMethods.getLast(split))
//        payload.put("redeUrl", split[split.size - 2])
//        return payload.toString()
//    }


   override fun crawlProductInfo(): JSONObject? {
      val lojaUrl = CommonMethods.getLast(homePage.split("www.nagumo.com.br").toTypedArray())
      val loadUrl = API_URL + "page/store" + lojaUrl
      var lojaId = ""
      var lojaRede: String? = ""
      val headers: MutableMap<String, String> = HashMap()
      headers["referer"] = homePage
      headers["accept"] = "application/json, text/plain, */*"
      headers["content-type"] = "application/json"
      val request = Request.RequestBuilder.create()
         .setUrl(loadUrl).setCookies(cookies)
         .setHeaders(headers)
         .setPayload(loadPayload)
         .build()
      val response = dataFetcher[session, request]
      val responseHeaders = response.headers
      if (responseHeaders.containsKey("sm-token")) {
         val header = responseHeaders["sm-token"]
         val token = JSONObject(header)
         lojaId = Integer.toString(JSONUtils.getIntegerValueFromJSON(token, "IdLoja", 0))
         if (lojaId == "0") {
            val body = JSONObject(response.body)
            lojaId = Integer.toString(JSONUtils.getValueRecursive(body, "sale.id", Int::class.java))
            lojaRede = Integer.toString(JSONUtils.getValueRecursive(body, "sale.idRede", Int::class.java))
            token.put("IdLoja", lojaId)
            token.put("IdRede", lojaRede)
         }
         headers["sm-token"] = token.toString()
      }
      val apiUrl = super.ApiSearchUrl(lojaId) + keywordEncoded
      val requestApi = Request.RequestBuilder.create()
         .setUrl(apiUrl)
         .setCookies(cookies)
         .setHeaders(headers)
         .build()
      return CrawlerUtils.stringToJson(dataFetcher[session, requestApi].body)
   }

   override fun getApiUrl(): String {
      return API_URL
   }

}
