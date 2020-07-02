package br.com.lett.crawlernode.crawlers.corecontent.espana

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.FetchUtilities
import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.fetcher.models.Response
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import models.Offer.OfferBuilder
import models.Offers
import models.pricing.Pricing
import org.apache.http.HttpHeaders
import org.json.JSONArray
import org.jsoup.nodes.Document
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher

class EspanaPrimenowCrawler(session: Session) : Crawler(session) {

  private val cep = "28010"

  private val userAgent = FetchUtilities.randUserAgent();

  init {
    super.config.fetcher = FetchMode.FETCHER;
  }

  override fun handleCookiesBeforeFetch() {
    val headers = mutableMapOf(HttpHeaders.USER_AGENT to userAgent);

    val requestOn: (String, Boolean) -> Response = { url, bodyIsrequired ->
      val request = RequestBuilder.create()
          .setUrl("https://primenow.amazon.es/$url")
          .setCookies(cookies)
          .setHeaders(headers)
          .setBodyIsRequired(bodyIsrequired)
				  .setFetcheroptions(FetcherOptionsBuilder.create().mustUseMovingAverage(false).build())
          .setProxyservice(
            listOf(
              ProxyCollection.INFATICA_RESIDENTIAL_BR,
              ProxyCollection.STORM_RESIDENTIAL_US,
              ProxyCollection.NETNUT_RESIDENTIAL_ES
            )
          )
          .build();
		
		  var resp = dataFetcher.get(
        session, request
      )
		
		  if(resp.body.isEmpty()) {
			   resp = ApacheDataFetcher().get(session, request);
			}

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

  override fun extractInformation(document: Document): MutableList<Product> {
    val products = mutableListOf<Product>()

    if (document.selectFirst("#productTitle") != null) {

      val name = document.selectFirst("#productTitle")?.text()?.trim()
      val description = document.selectFirst("#productDescription_feature_div")?.html()
      val primaryImg = document.selectFirst("#landingImage")?.attr("data-old-hires")
      val secondaryImgs = JSONArray()
      document.select(".a-spacing-small.item img")?.eachAttr("src", arrayOf(0))
        ?.forEach { img -> secondaryImgs.put(img.replaceFirst("38,50", "640,480")) }
      val internalId = session.originalURL.substringAfter("dp/").substringBefore("?")

      val offers = scrapOffers(document)

      products += ProductBuilder.create()
        .setUrl(session.originalURL)
        .setInternalId(internalId)
        .setName(name)
        .setPrimaryImage(primaryImg)
        .setSecondaryImages(secondaryImgs.toString())
        .setDescription(description)
        .setOffers(offers)
        .build()
    }

    return products
  }

  private fun scrapOffers(document: Document): Offers {
    val offers = Offers()

    val price = document.selectFirst("#priceblock_ourprice")?.toDoubleComma()
    val priceFrom = document.selectFirst(".priceBlockStrikePriceString")?.toDoubleComma()


    val pricing = Pricing.PricingBuilder.create()
      .setPriceFrom(priceFrom)
      .setSpotlightPrice(price)
      .setBankSlip(price?.toBankSlip()).build()
    val offer = OfferBuilder.create()
      .setUseSlugNameAsInternalSellerId(true)
      .setPricing(pricing)
      .setSellerFullName(document.selectFirst("#merchant-info")?.text()?.trim()?.substringAfter("por "))
      .setIsBuybox(false)
      .setIsMainRetailer(true)
      .build()

    offers.add(offer)

    return offers
  }
}
