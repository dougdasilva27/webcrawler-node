package br.com.lett.crawlernode.crawlers.corecontent.portoalegre

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.models.CategoryCollection
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.models.RequestMethod
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.CommonMethods
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.Logging
import models.Offers
import org.apache.http.impl.cookie.BasicClientCookie
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class PortoalegreAsunCrawler constructor(session: Session?) : Crawler(session) {



   override fun handleCookiesBeforeFetch() {
//      val request = Request.RequestBuilder.create()
//         .setUrl("https://loja.asun.com.br/localiza")
//         .setPayload(cepPayload())
//         .setCookies(cookies)
//         .build()

//      cookies.addAll(dataFetcher.get(session,request).cookies)


//
//      val request = Request.RequestBuilder.create()
//         .setUrl("https://loja.asun.com.br")
//         .setCookies(cookies)
//         .build()
//
//      cookies.addAll(dataFetcher.get(session, request).cookies)


   }

   override fun fetch(): Any {

      cookies.add(BasicClientCookie("storeSelected","eyJpdiI6InNyVkJ0WDRiMUJMWXF4TDlJSEhselE9PSIsInZhbHVlIjoieTl5K1hCcWR5MnlDaFo3TXBPWDFFdz09IiwibWFjIjoiYTczOWRiMDNkMzJjNTg0NjA0ZGFlNjQzNzNjMTYwZjcwNjFmZWM3MWMwOTI3YzExZmI4OTgxMmMxOWJkNjMzZSJ9"))
      cookies.add(BasicClientCookie("XSRF-TOKEN","eyJpdiI6Ilo1VzA3MTA2MFJOdTE5MUxNd0ZFWEE9PSIsInZhbHVlIjoiTENuSnI0Y3gyVlFlN2RNRjVQTUZXMmtEOW01U0JjUUpOZTVSTUdrdUorcVdZUDhOTHFwdHJlRTRMY3RIOVMrXC8iLCJtYWMiOiIyOWI3MWVmODM1NzhmNmQzOGMyMmU0OTdiZTRhZmRhMTk2MjBhYmY4MTA4M2QzMmE0OTIzZGM2NmE4YTEzNDhjIn0"))
      cookies.add(BasicClientCookie("laravel_session","eyJpdiI6IjB6MkdDaVhWMFJSUEVFMVUrc2JXd2c9PSIsInZhbHVlIjoic1drWVJTdStYbDhjbEowYWt1NnhKQjFZd3hWdlptR0FPUEVoQ3RxM2ZCWUpKRW5RZExuQSt5eTVDb21oV2NWWSIsIm1hYyI6ImFkNTNhYTA2ZTE3OGM4MDFiNDFmOTU5YjgxMjc4Y2NkZmRjNGZiYzFiOTc5NjVmMjgxMzE5MWE0ZGI0NTg4YjIifQ"))
      cookies.add(BasicClientCookie("userLocation","eyJpdiI6Ik00SmszaUl5a3VodVhcL01yZVwvbzVTdz09IiwidmFsdWUiOiJoODZHenhUWlhwdFowbm9hVlljczhiTkdGUWFDY0h4cVpvRm80UVVxTDJBcWVVRnFNWjlSOWVrd0lheDhDVkV4NXUwOFJBRkZEcXl3a1RUYnV6bTNPcmZZbnpQc1M4VjhvMzlDQk81Y0RHSXJZOXhyNFI5RDEwODRrdFUzSFJZZHl1bnc3ZWp6OWtCZHBYWUlSbndwY0txZzFkODZJeENZRzNuczZpbk9WVU5FSjRYMmZFTVwvRm5kMXF4UTVQTVRcL2ZRb1c3aEIxWjlSNXNwMFBWUTZpdVcyS2pIZVU4QkRsdnB1OXJFNWZFcHc9IiwibWFjIjoiZTkyYzQzYWJhOTc0NGIxYTViNTRjMzgyOWUwOWQzMDI3NGE4ZGM4MDY2YjAzOTlmNWJmYTA0ZTMxNzM2MDFmZCJ9"))
      cookies.add(BasicClientCookie("userMethodDelivery","eyJpdiI6IjdjWVwvcUV2RTlNeTRySDhMN3BJOTh3PT0iLCJ2YWx1ZSI6ImdKQXBPUlg3Z3IrR05seGltWk1jeHc9PSIsIm1hYyI6IjYzNzc2NmUxNGM4MzVhMzk0NGFmY2UwNzVjMDE4MTFhMTE5Zjc4ZmQ4Mjg1YTNmZGVmNWQ4OTYyM2M1MTU4NzcifQ%3D%3D"))
      cookies.add(BasicClientCookie("USR_TOKEN","eyJpdiI6InQrUHhoTG1xVnUxVkdBZnZBUjA0dmc9PSIsInZhbHVlIjoiZUpGaDdnazhnODlLcTh0SGt4QWo0RUhUWGpuTGs3V1hKMmlER3Y5OVdcL2xZc1A3MDFOeHFIM042RkFOVUFlcW1OR09sXC9FQzg5QUpucXN1K3BPZWZRdz09IiwibWFjIjoiMmUzZTdhN2ZkZjFhZWI4MTA2NzFiNjE2ZDIyMTEzMTA5MTFkZGQ2ZGZhYzBkNTQ4NzNmYjI1N2FlYmM4ZWQwNCJ9"))
      cookies.add(BasicClientCookie("storeLocationCheck","eyJpdiI6InhldWdHVlVsVXFnTEhxQUtpNzZkb0E9PSIsInZhbHVlIjoiZlpwXC9cL3pHTUFGNEMwRDFxWW9lV2NnPT0iLCJtYWMiOiI5NGE3Y2Y0M2RhYmVkZGMxZDAzNWFlMzBhNThjYTQ2NDQwMWQyMmNmMTA4OTIyNWU5ZWVjODViZGFiYmRjNGQ5In0%3D"))



//      val requestCookies = Request.RequestBuilder.create()
//         .setUrl("https://loja.asun.com.br/localizacao/cep/91740-001")
//         .setCookies(cookies)
//         .build()
//
//      cookies.addAll(this.dataFetcher.get(session,requestCookies).cookies)

      var headers = mapOf("cookies" to CommonMethods.cookiesToString(cookies))

      var request = Request.RequestBuilder.create()
         .setHeaders(headers)
         .setUrl(session.originalURL)
         .build()

      return Jsoup.parse(this.dataFetcher.get(session, request).body)
   }

   private fun cepPayload(): String {
      val cep = session.options.optString("cep")
      val requestToken: Request = Request.RequestBuilder()
         .setUrl("https://loja.asun.com.br/")
         .build()
      var resposeToken = dataFetcher[session, requestToken]
//      this.cookies.addAll(resposeToken.cookies)
      val doc = Jsoup.parse(resposeToken.body)
      val token = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "meta[name=\"csrf-token\"]", "content")
      val cepRequest: Request = Request.RequestBuilder()
         .setUrl("https://loja.asun.com.br/localizacao/cep/$cep")
         .build()
      var resposeCep = dataFetcher[session, cepRequest]
      this.cookies.addAll(resposeCep.cookies)
      val obj = CrawlerUtils.stringToJson(resposeCep.body)
      val address = obj.optJSONObject("address")
      val payload = "cep=" + cep +
         "&logradouro=" + address.optString("logradouro", "") +
         "&numero=" +
         "&bairro=" + address.optString("nome_bairro", "") +
         "&cidade=" + address.optString("localidade", "") +
         "&uf=" + address.optString("uf", "") +
         "&location%5Blat%5D=" + address.optJSONObject("location").optInt("lat") +
         "&location%5Blng%5D=" + address.optJSONObject("location").optInt("lng") +
         "&idestabelecimento=" +
         "&hasMultDelivery=false" +
         "&_token=" + token
      return payload.replace(" ".toRegex(), "+")
   }

   override fun extractInformation(document: Document?): MutableList<Product> {
      val products: MutableList<Product> = ArrayList()

      if (true) {
         Logging.printLogDebug(logger, session, "Product page identified: " + session.originalURL)
         val internalId: String = CrawlerUtils.scrapStringSimpleInfo(document, ".stock", true)?.replace("[^0-9]", "")!!.trim();
         val internalPid: String = internalId
         val name: String? = document?.selectFirst(".details strong[itemprop=\"name\"]")?.text()
         val categories: CategoryCollection = CrawlerUtils.crawlCategories(document, ".row.bcrumb")

         val images = crawlImages(document)
         val primaryImage: String = images.removeAt(0)
         val secondaryImages: MutableList<String> = images

         val availableToBuy: Boolean = true
         val offers = if (availableToBuy) scrapOffer(document, internalId) else Offers()

         // Creating the product
         val product = ProductBuilder.create()
            .setUrl(session.originalURL)
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setOffers(offers)
            .build()
         products.add(product)
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.originalURL)
      }

      return products


   }

   private fun crawlImages(document: Document?): MutableList<String> {
      val jsonString = document?.selectFirst("product-images-component")?.text()

      return mutableListOf();
   }

   private fun scrapOffer(doc: Document?, internalId: String): Offers {
      return Offers()
   }

}
