package br.com.lett.crawlernode.crawlers.ranking.keywords.espana

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.FetchUtilities
import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.fetcher.models.Response
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.toDoc
import br.com.lett.crawlernode.util.toInt
import br.com.lett.crawlernode.util.toJson
import org.apache.http.HttpHeaders
import org.jsoup.nodes.Element

class EspanaPrimenowCrawler(session: Session) : CrawlerRankingKeywords(session) {

  private val cep = "28010"

  private val userAgent = FetchUtilities.randUserAgent();

  init {
    pageSize = 30
    fetchMode = FetchMode.FETCHER;
  }

  override fun processBeforeFetch() {
    val headers = mutableMapOf(HttpHeaders.USER_AGENT to userAgent);

    val requestOn: (String, Boolean) -> Response = { url, bodyIsrequired ->
      val resp = dataFetcher.get(
        session, Request.RequestBuilder.create()
          .setUrl("https://primenow.amazon.es/$url")
          .setCookies(cookies)
          .setHeaders(headers)
          .setBodyIsRequired(bodyIsrequired)
          .setProxyservice(
            listOf(
              ProxyCollection.INFATICA_RESIDENTIAL_BR,
              ProxyCollection.STORM_RESIDENTIAL_EU,
              ProxyCollection.STORM_RESIDENTIAL_US
            )
          )
          .build()
      )
      if (resp.cookies.size > 0) {
        cookies = resp.cookies
      }
      resp
    }

    val resp = requestOn("onboard", true)
    val token = resp.body?.toDoc()?.selectFirst("span[data-location-select-form-submit]")
      ?.attr("data-location-select-form-submit")?.toJson()?.optString("offerSwappingToken")!!

    requestOn("onboard/check?postalCode=$cep&offerSwappingToken=$token", false)
    requestOn("cart/initiatePostalCodeUpdate?newPostalCode=$cep&allCartItemsSwappableUrl=%2Fhome&noCartUpdateRequiredUrl=%2Fhome&someCartItemsUnswappableUrl=%2Fhome&offer-swapping-token=$token", false)
  }


  override fun extractProductsFromCurrentPage() {
    currentDoc = fetchDocument("https://primenow.amazon.es/search?k=$keywordEncoded&ref_=pn_gw_nav_sr_ALL&page=$currentPage")
    for (element: Element in currentDoc.select(".asin_card__root__3x1lV")) {
      val url = element.selectFirst("a")?.attr("href")
      val internalId = url?.substringBefore("?")?.substringAfter("dp/")
      saveDataProduct(internalId, null, "https://primenow.amazon.es$url")
      log("internalId - $internalId url - https://primenow.amazon.es$url")
    }
  }

  override fun setTotalProducts() {
    totalProducts = currentDoc.selectFirst(".index__root__3XLxs div")?.toInt() ?: 0
  }
}