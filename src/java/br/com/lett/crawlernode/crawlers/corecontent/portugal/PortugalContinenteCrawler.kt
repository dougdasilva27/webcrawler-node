package br.com.lett.crawlernode.crawlers.corecontent.portugal

import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.toBankSlip
import br.com.lett.crawlernode.util.toDoubleComma
import models.Offer.OfferBuilder
import models.Offers
import models.pricing.Pricing.PricingBuilder
import org.jsoup.nodes.Document
import br.com.lett.crawlernode.util.CommonMethods
import br.com.lett.crawlernode.test.Test
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher
import org.jsoup.Jsoup
import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.util.Logging
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher
import br.com.lett.crawlernode.core.fetcher.methods.JavanetDataFetcher
import java.util.Arrays

class PortugalContinenteCrawler(session: Session) : Crawler(session) {

	override fun fetch() : Document {
		
		 val headers =  mutableMapOf<String, String?>()
		
		 // this will work for a short time, we need another solution!!
		 headers.put("cookie", "GCLB=CP_3z4bM6M2_8gE; rbzid=N9FmHzzU0zn3RmiaPjnOOXDuHpqeByEPuJd93/SiszJphdTgJ4euaLjGs2a4atQ/0J9xtoSGH90klfcO/FpciJ5/1" +
				 "y4Q5IGnjuWrtyDCM6mcVeSvYGFbxxPYooSFt2RxxvG5VjaJgeBvlfqU+8HtyNrue+DFUey9sgM18iPmxoZhBC/Ezm7EGSRLvqM+NWhQdmHmR6CiNfwgqxbpPhLjTqZ8OLkorqK+Y9HID/D4ZtaR" +
				 "CIjngexd0RstEiYIN13HTPyNYi9XjbBG+PNdyJMHNQ==; rbzsessionid=4673fa3d131c1edbc4908b0872b47060");
		 headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.135 Safari/537.36");
		
		 val request = RequestBuilder.create()
			 .setHeaders(headers)
			 .setUrl(session.getOriginalURL())
			 .setProxyservice(Arrays.asList(ProxyCollection.INFATICA_RESIDENTIAL_BR_HAPROXY))
			 .build();
     val response = JavanetDataFetcher().get(session, request);
		
		 return Jsoup.parse(response.getBody());
	}
	
  override fun extractInformation(document: Document): MutableList<Product> {
    val products = mutableListOf<Product>()

    if (document.selectFirst(".productInfoArea") != null) {

      val name = document.selectFirst(".productTitle")?.text()
      val internalId = document.selectFirst(".ProductCode")?.attr("value")
      val description = document.selectFirst(".productDetailArea .productDetailSubArea")?.html()

      val offers = scrapOffers(document)

      val primaryImg = document.selectFirst("#bigProduct")?.attr("href")

      products += ProductBuilder.create()
        .setUrl(session.originalURL)
        .setInternalId(internalId)
        .setName(name)
        .setPrimaryImage(primaryImg)
        .setDescription(description)
        .setOffers(offers)
        .build()

    } else {
            Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }
    return products
  }

  fun scrapOffers(doc: Document): Offers {
    val offers = Offers()

    val price = doc.selectFirst(".updListPrice")?.toDoubleComma()
    val priceFrom = doc.selectFirst(".priceWas .pricePerUnit")?.toDoubleComma()

	  CommonMethods.saveDataToAFile(doc, Test.pathWrite + "CONTINENTE.html");
	  
    val bankSlip = price?.toBankSlip()
    val pricing = PricingBuilder.create()
      .setSpotlightPrice(price)
      .setPriceFrom(priceFrom)
      .setBankSlip(bankSlip)
      .build()

    offers.add(
      OfferBuilder.create()
        .setIsBuybox(false)
        .setIsMainRetailer(true)
        .setPricing(pricing)
        .setUseSlugNameAsInternalSellerId(true)
        .setSellerFullName("Continente")
        .build()
    )
    return offers
  }
}
