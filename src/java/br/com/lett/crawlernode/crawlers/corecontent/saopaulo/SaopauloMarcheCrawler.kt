package br.com.lett.crawlernode.crawlers.corecontent.saopaulo

import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher
import br.com.lett.crawlernode.core.fetcher.methods.JavanetDataFetcher
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.JSONUtils
import br.com.lett.crawlernode.util.Logging
import models.prices.Prices
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.lang.StringBuilder
import java.math.RoundingMode
import java.net.URLEncoder

class SaopauloMarcheCrawler(session: Session?) : Crawler(session) {

   private val home = "https://www.marche.com.br/"
   private val cep = "05303000"

   override fun handleCookiesBeforeFetch() {
//      val request : Request = Request.RequestBuilder.create()
//         .setUrl(home)
//         .build()
//      val response = dataFetcher.get(session,request)
//      this.cookies = response.cookies
//      val token = URLEncoder.encode(getToken(response.body),"UTF-8")
////      var payload = "utf8=%E2%9C%93&authenticity_token=fw0siDnqaIfXGsxwXIF2AScOKbC%2F60QnOYbdSzfdPWhJ6hCTo%2FbyTLcTAg1gO%2Fd9p5%2F9ErwbJCU7JwoumSJKcQ%3D%3D&zip_code%5Bcode%5D=05303000&commit=Enviar"
//      var payload = "utf8=%E2%9C%93&authenticity_token="+token+"&zip_code%5Bcode%5D="+cep+"&commit=Enviar"
//      var cookies : StringBuilder = StringBuilder()
//      for (cookie in this.cookies) {
//         cookies.append(cookie.name)
//         cookies.append("=")
//         cookies.append(cookie.value)
//         cookies.append("; ")
//      }



      val headers = mutableMapOf<String,String>()
      headers["cookie"]= "ahoy_visitor=feba790e-8273-44a1-b1aa-389a8deba965; device=MTQ5NDA3ODc%3D--a104e21f00b17910052fc5869c241c7185f65224; ajs_anonymous_id=%22feba790e-8273-44a1-b1aa-389a8deba965%22; back_location=Imh0dHBzOi8vd3d3Lm1hcmNoZS5jb20uYnIvIg%3D%3D--ae660913c666a399ff57d21c892a9ac5d14cdc33; blueID=c9c629c9-2cc5-48c2-bbdc-ebb0e10866f3; _fbp=fb.2.1609771736370.1876192171; _ga=GA1.3.1327364073.1609771736; _gid=GA1.3.126479532.1609771736; __kdtv=t%3D1609336291551%3Bi%3D3ff1ca2a912d9f450c2f37453d20bb4036ac880f; _kdt=%7B%22t%22%3A1609336291551%2C%22i%22%3A%223ff1ca2a912d9f450c2f37453d20bb4036ac880f%22%7D; _hjTLDTest=1; _hjid=d16f8350-f741-4132-982c-60ea1fa5b9f6; _hjIncludedInPageviewSample=1; _st_marche_ecomm_session=RFlCOG0rSjdmTGdmQWVPSEJlbmh1ZWtYbDVxemRRTTIxejRDQmpoZGdGbmtPWkVvL3ZvcXJxRmtOM2lDdXRFVm1IMW1JemYyRWovZVZlOWU0Z3lSY3ZVc3dNWnBBOVJTeXNlNEFFeHI4eDJaOTBPSzVZVUQzWjA0YnhJY0JJWmE4eXl4dEpuTmZiN0c0NGpFdTJ0MDdRPT0tLVhRcXllMFV2Y0FPSS9FcitPcG1tT1E9PQ%3D%3D--1657d22dbea0abfd19b5825e75d67ad4539e51f6"
      headers["Referer"]="https://www.marche.com.br/"
      headers["Content-Type"]="application/x-www-form-urlencoded"
//      headers["Host"]="www.marche.com.br"
//      headers["User-Agent"]= "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.66 Safari/537.36"
//      val headers = mutableMapOf<String,String>("cookie" to cookies.toString())
      val   payload = "utf8=%E2%9C%93&authenticity_token=fw0siDnqaIfXGsxwXIF2AScOKbC%2F60QnOYbdSzfdPWhJ6hCTo%2FbyTLcTAg1gO%2Fd9p5%2F9ErwbJCU7JwoumSJKcQ%3D%3D&zip_code%5Bcode%5D=05303000&commit=Enviar"
      val requestvalidate :Request = Request.RequestBuilder.create()
         .setUrl("https://www.marche.com.br/deliverable_zip_codes/set_zipcode")
//         .setCookies(this.cookies)
         .setHeaders(headers)
         .setPayload(payload)
         .build()
      val responsevalidade =  dataFetcher.post(session,requestvalidate)
      this.cookies = responsevalidade.cookies

   }
   private fun getToken(html:String):String{
      val document = Jsoup.parse(html)
      return CrawlerUtils.scrapStringSimpleInfoByAttribute(document,"head  meta[name=csrf-token]","content")
   }

   override fun shouldVisit(): Boolean {
      val href = session.originalURL.toLowerCase()
      return !FILTERS.matcher(href).matches() && href.startsWith(home)
   }

   override fun extractInformation(document: Document?): MutableList<Product> {
      val products = mutableListOf<Product>()
      val json = JSONUtils.stringToJson(CrawlerUtils.scrapStringSimpleInfoByAttribute(document, "div[data-json]",
         "data-json"))

      json ?: Logging.printLogDebug(logger, session, "Not a product page " + session.originalURL)
      val price = json?.optFloat("price")
      if (price == null || price.isNaN()) {
         return products
      }
      json.let {

         val prices = scrapPrices(document, price)
         val categories = mutableListOf<String>()

         if (it.optString("parent_category") is String) {
            categories.add(it.optString("parent_category"))
         }
         if (it.optString("category") is String) {
            categories.add(it.optString("category"))
         }

         val eans = mutableListOf<String>()
         if (it.optString("ean") is String) {
            categories.add(it.optString("ean"))
         }
         val primaryImage = document?.selectFirst(".product-image img")?.attr("src")

         products.add(ProductBuilder.create()
            .setUrl(session.originalURL)
            .setInternalId(it.optString("product_id"))
            .setInternalPid(it.optString("id"))
            .setName(it.optString("full_name"))
            .setPrice(price)
            .setPrices(prices)
            .setAvailable(document?.selectFirst(".btn.btn-block.btn-lg.center-y") != null)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setEans(eans)
            .build())
      }
      return products
   }

   private fun scrapPrices(doc: Document?, price: Float) = Prices().apply {
      this.priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-price> span", null, false, ',', session)
      this.bankTicketPrice = price.toBigDecimal().setScale(2, RoundingMode.HALF_EVEN).toDouble()
      val installments: MutableMap<Int, Float> = HashMap()
      installments[1] = price
      this.insertCardInstallment(Card.VISA.toString(), installments)
      this.insertCardInstallment(Card.MASTERCARD.toString(), installments)
      this.insertCardInstallment(Card.ELO.toString(), installments)
   }
}
