package br.com.lett.crawlernode.crawlers.corecontent.belgium

import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.models.CategoryCollection
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import exceptions.MalformedPricesException
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.*

abstract class ColruytCrawler(session: Session) : Crawler(session) {
   

   /*
   * para pegar o placeId tem que entrar na home do site https://www.colruyt.be/fr
   * ir em Liste de courses no lado direito da tela, colocar o `code postal` ou o endereço
   * e escolher a unidade em Sélectionné e CONFIRMER.
   * Depois, escolher o produto.
   *
   * Apos isso, para pegar o placeId, abre a ferramenta network do navegador e procura pela requisição:
   * https://ecgproductmw.colruyt.be/ecgproductmw/v2/fr/products/0000?clientCode=clp&placeId=????
   *
   * */
   abstract fun getPlaceId(): String

   override fun fetch(): JSONObject? {
      val productId = CommonMethods.getLast(session.getOriginalURL().split("pid="));

      val headers: MutableMap<String, String> = HashMap()
      headers["User-Agent"] = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.80 Safari/537.36"
      headers["Origin"] = "https://www.colruyt.be"
      headers["Cookie"] = "rxVisitor=1608312692395CFPOBA48207MG1EH5RHBSEB1UBCOII1V; _ga=GA1.2.734203397.1608313962;" +
         "_gid=GA1.2.2037973239.1608313962; AMCVS_FA4C56F358B81A660A495DE5%40AdobeOrg=1; DG_ZID=977435AD-6ADB-3FBE-890F-8AC717022504;" +
         " DG_ZUID=2DB92570-3C1D-33CF-9288-9F1ACFDD44E1; DG_HID=8BF72FF8-C828-3B9F-A8A0-BECFAF388B55;" +
         " DG_SID=194.71.227.125:CgpMyhxmbfP5K6h7cVTMw4pRG95s43AI2oIchd+GPHA; " +
         "TS0113bcfc=016303f955bc7067f591743a1dcf2ced11032b264a91277c1d29b01e603edac6104978d4ee64fcbf9fd58c377baab703d8c591e032; " +
         "s_ecid=MCMID%7C65043871162792934330879339047415423361; " +
         "AMCV_FA4C56F358B81A660A495DE5%40AdobeOrg=1406116232%7CMCIDTS%7C18615%7CMCMID%7C65043871162792934330879339047415423361%7CMCAAMLH" +
         "-1608918761%7C6%7CMCAAMB-1608918761%7CRKhpRz8krg2tLO6pguXWp5olkAcUniQYPHaMWWgdJ3xzPWQmdj0y%7CMCOPTOUT-1608321161s%7CNONE%7CMCAID%7CNONE%7" +
         "CvVersion%7C2.5.0; s_cc=true; DG_IID=9C0777D2-9F9C-30AE-96D4-ACFFF5642237; DG_UID=00EFF1A0-FC94-37F7-BEC3-A14C52125972; " +
         "s_fid=2CC14288DAC7A67D-12BF35782CE0F12A; s_vi=[CS]v1|2FEE76368515DC3E-40000673243D929B[CE]; dtSa=-; " +
         "OptanonAlertBoxClosed=2020-12-18T17:52:52.646Z; OptanonConsent=isIABGlobal=false&datestamp=Fri+Dec+18+2020+14%3A52%3A52" +
         "+GMT-0300+(Brasilia+Standard+Time)&version=6.6.0&hosts=&landingPath=NotLandingPage&AwaitingReconsent=false&groups=" +
         "C0001%3A1%2CC0003%3A1%2CC0002%3A1%2CC0004%3A1; s_sq=%5B%5BB%5D%5D; _uetsid=e163dbf0415611ebbfbaf3993f8b40b2; " +
         "_uetvid=c5f799c0358d11eb9e03fd0ec3ce7fbd; _hjTLDTest=1; _hjid=892fefc8-960d-45fc-8a9b-14051445d50d; _hjAbsoluteSessionInProgress=0;" +
         " _pin_unauth=dWlkPU5qTmtPVFE0TnpBdE5XSXlOUzAwTkRNekxUbGhNR1V0Wm1ZM05EaG1ZV00xTVRBNQ;" +
         "dtCookie=8$795A23D5556E8A717DE8E1688B019AED|b84fed97a8123cd5|0; dtLatC=721; at_check=true; mbox=session#c1b30a24f85b462bae0a033de8c6665d#1608316161;" +
         " tms_storevisit=eyJ1c2VyX3Zpc2l0X2lkIjoiNTE0NjcyLjE2MDgzMTM5NjA5NDUiLCJsYXN0X2xvZ2luX3N0YXRlIjoibm8iLCJwYWdlX2FkYmxvY2siOiJub3RhY3RpdmUifQ%3D%3D; " +
         "utag_main=v_id:017676fb79f900085cd7561bfefc03068001906000bd0\$_sn:1\$_se:18\$_ss:0\$_st:1608316101735\$ses_id:1608313960957%3Bexp-session\$_pn:4%3" +
         "Bexp-session\$vapi_domain:colruyt.be; rxvt=1608316102382|1608312692397; dtPC=8$314299635_283h1vWPMJPIEPWHKUASPNGIBKRHCUFAPGNHMM-0"

      val url = "https://ecgproductmw.colruyt.be/ecgproductmw/v2/fr/products/${productId}?clientCode=clp&placeId=${getPlaceId()}&dataGroup=ALL"

      val result = dataFetcher.get(
         session, RequestBuilder.create()
            .setUrl(url)
            .setHeaders(headers)
            .setProxyservice(
               listOf(
                  ProxyCollection.LUMINATI_SERVER_BR,
                  ProxyCollection.BONANZA_BELGIUM,
                  ProxyCollection.NETNUT_RESIDENTIAL_ES
               )
            )
            .build()
      )?.body

      return result?.toJson()
   }


   override fun extractInformation(jsonObject: JSONObject): MutableList<Product> {
      val products = mutableListOf<Product>()

      if (jsonObject.has("commercialArticleNumber")) {
         Logging.printLogInfo(logger, session, "Product page identified: " + session.originalURL)


         products += product {
            url = session.originalURL
            categories = scrapCategories(jsonObject)
            name = "${jsonObject.optString("brand")} ${jsonObject.optString("name")} ${jsonObject.optString("content")}".trim()
            description = getDescripton(jsonObject)
            primaryImage = jsonObject.optString("fullImage")
            internalId = jsonObject.optString("commercialArticleNumber")
            internalPid = jsonObject.optString("productId")

            if (jsonObject.optBoolean("isAvailable")) {
               offer {
                  isMainRetailer
                  useSlugNameAsInternalSellerId
                  sellerFullName = "Colruyt"
                  pricing {
                     val price = jsonObject.optJSONObject("price")?.optDouble("basicPrice")
                     spotlightPrice = price
                     bankSlip = price?.toBankSlip() ?: throw MalformedPricesException()
                  }
               }
            } else {
               offer { }
            }
         }
      } else {
         Logging.printLogInfo(logger, session, "not a product page: " + session.originalURL)
      }
      return products
   }

       private fun fetcherPage(json: JSONObject) : org.jsoup.nodes.Document? {

          val url = json.optString("ficUrl")
          val request = Request.RequestBuilder.create()
             .setUrl(url)
             .setProxyservice(Arrays.asList( ProxyCollection.LUMINATI_SERVER_BR,
                ProxyCollection.BONANZA_BELGIUM,
                ProxyCollection.NETNUT_RESIDENTIAL_ES))
             .build()

          return Jsoup.parse(dataFetcher.get(session, request).body)

   }

   private fun getDescripton(json: JSONObject) : String {
      val doc = fetcherPage(json)
      return CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".row .col-xs-12.col-sm-12.col-md-12.col-lg-12 p"))
   }




   fun scrapCategories(json: JSONObject): CategoryCollection {

      val categories = CategoryCollection()

      val jsonArray = JSONUtils.getJSONArrayValue(json, "categories")

      if (jsonArray.isEmpty) {
         return categories
      }
      var currentCategory: Any? = jsonArray[0]
      for (x in 0..2) {

         if (currentCategory == null) {
            break
         }

         if (currentCategory is JSONObject) {
            val name = currentCategory.optString("name")
            if (name != null) {
               categories.add(name)
               val children = currentCategory.optJSONArray("children")
               currentCategory = children?.get(0)
            } else {
               break
            }
         }
      }

      return categories
   }

   fun waitLoad(time: Int) {
      try {
         Thread.sleep(time.toLong())
      } catch (e: InterruptedException) {
         e.printStackTrace()
      }
   }
}
