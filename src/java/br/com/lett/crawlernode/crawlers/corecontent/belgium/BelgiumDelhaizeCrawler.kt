package br.com.lett.crawlernode.crawlers.corecontent.belgium

import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import models.Offer
import models.Offers
import models.pricing.Pricing
import org.jsoup.nodes.Document
import org.jsoup.Jsoup
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.test.Test
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher
import br.com.lett.crawlernode.core.fetcher.methods.JavanetDataFetcher

/**
 * Date: 30/07/20
 *
 * @author Fellype Layunne
 *
 */

class BelgiumDelhaizeCrawler(session: Session) : Crawler(session) {

   companion object {
      const val SELLER_NAME: String = "Delhaize"
   }

	 override fun fetch(): Document {
		  dataFetcher = FetcherDataFetcher();
		 
		  val headers: MutableMap<String, String> = HashMap()
		  headers["user-agent"] = "Mozilla/5.0 (X11; Linux x86_64; AppleWebKit/537.36 (KHTML] = like Gecko; Chrome/86.0.4240.75 Safari/537.36";
		 
	    val result = dataFetcher.get(
         session, RequestBuilder.create()
         .setUrl("https://www.delhaize.be/fr-be/")
				 .setHeaders(headers)
         .build()
      );
		 
		 
		 var jsessionId :String? = null; 
		 
		 for(cookie in result.getCookies()) {
  			if(cookie.getName().equals("JSESSIONID")) {
    				jsessionId = cookie.getValue();
    				break;
  			} 
		 }
		 
		 val homeDocument = Jsoup.parse(result?.body);
		 
		 val token = CrawlerUtils.scrapStringSimpleInfoByAttribute(homeDocument, "#CSRFTokenMaster", "value");
		 
		 headers["cookie"] = "JSESSIONID=${jsessionId};"
		 headers["authority"] = "www.delhaize.be";
     headers["cache-control"] = "max-age=0";
     headers["upgrade-insecure-requests"] = "1";
     headers["origin"] = "https://www.delhaize.be";
     headers["content-type"] = "application/x-www-form-urlencoded";
     headers["accept"] = "text/html] =application/xhtml+xml] =application/xml;q=0.9] =image/avif] =image/webp] =image/apng] =*/*;q=0.8] =application/signed-exchange;v=b3;q=0.9";
     headers["sec-fetch-site"] = "same-origin";
     headers["sec-fetch-mode"] = "navigate";
     headers["sec-fetch-user"] = "?1";
     headers["sec-fetch-dest"] = "document";
     headers["referer"] = "https://www.delhaize.be/fr-be/collectionpoint";
     headers["accept-language"] = "pt-BR] =pt;q=0.9] =en-US;q=0.8] =en;q=0.7] =es;q=0.6";
		 
		 val payload = "posName=10014037&warehouseCode=10014037&CSRFToken=${token}"
		 
		 System.err.println(jsessionId);
		 System.err.println(token);
		 
		 dataFetcher.post(
         session, RequestBuilder.create()
         .setUrl("https://www.delhaize.be/storelocator/selectAsCollectionPoint?lastViewedPage=%252Fstorelocator%253Fintcmp%253Dlocatormenu%2523query%25253D1500&goBackAfterRegistration=true")
				 .setFollowRedirects(false)
				 .setHeaders(headers)
				 .setPayload(payload)
				 .mustSendContentEncoding(false)
         .build()
      );
		 
		 val headers2: MutableMap<String, String> = HashMap()
		 headers2["cookie"] = "JSESSIONID=${jsessionId};"
		 
		  val response = dataFetcher.get(
         session, RequestBuilder.create()
         .setUrl(session.originalURL)
         .setHeaders(headers2)
         .build()
      )?.body;
		 
		 return Jsoup.parse(response);
	 }
	
   override fun extractInformation(doc: Document): MutableList<Product> {
      super.extractInformation(doc)

	      CommonMethods.saveDataToAFile(doc, Test.pathWrite + "DELHAIZE.html");
	   
      if (!isProductPage(doc)) {
         return mutableListOf()
      }

      val name = doc.selectFirst(".product-details .page-title")?.text()
      val internalId = doc.selectFirst(".product-details .Product.component")?.attr("data-item-id")

      val available = doc.selectFirst(".ProductDetails .ProductBasketManager .ProductBasketAdder") != null

      val description = doc.selectFirst(".ShowMoreLess__content")?.html()

      val categories = scrapCategories(doc)

      val primaryImage = scrapPrimaryImage(doc)
      val secondaryImages = scrapSecondaryImages(doc)

      val offers = if (available) scrapOffers(doc) else Offers()

      val product = ProductBuilder()
         .setUrl(session.originalURL)
         .setInternalId(internalId)
         .setInternalPid(internalId)
         .setName(name)
         .setCategories(categories)
         .setPrimaryImage(primaryImage)
         .setSecondaryImages(secondaryImages)
         .setDescription(description)
         .setOffers(offers)
         .setRatingReviews(null)
         .build()

      return mutableListOf(product)
   }

   private fun scrapPrimaryImage(doc: Document): String {
      val data = doc.selectFirst(".magnifyWrapper div").attr("data-media")

      val json = JSONUtils.stringToJson(data)

      json?.keys()?.forEach {
         val image = json.optString(it)
         if (image?.isNotEmpty() == true) {
            return "https:${image}"
         }
      }
      return ""
   }

   private fun scrapSecondaryImages(doc: Document): List<String> {
      val images = mutableListOf<String>()

      doc.select(".magnifyWrapper div").filterIndexed { i, _ -> i > 0 }.map { it ->

         val data = it.attr("data-media")

         val json = JSONUtils.stringToJson(data)

         json?.keys()?.forEach {
            val image = json.optString(it)
            if (image?.isNotEmpty() == true) {
               images += "https:${image}"
            }
         }
      }
      return images
   }

   private fun scrapCategories(doc: Document): Collection<String> {
      return doc.select(".Breadcrumb a:not(.home)").eachAttr("title", arrayOf(0))
   }

   private fun scrapOffers(doc: Document): Offers {
      val offers = Offers()

      var price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".ProductDetails .ultra-bold.test-price-property > span:last-child", null, false, ',', session)

      val sales = doc.select(".ProductDetails .ProductPromotions .text-bold").eachText()

      val bankSlip = price.toBankSlip()

      val creditCards = listOf(Card.MASTERCARD, Card.VISA).toCreditCards(price)

      offers.add(
         Offer.OfferBuilder.create()
            .setPricing(
               Pricing.PricingBuilder.create()
                  .setSpotlightPrice(price)
                  .setCreditCards(creditCards)
                  .setBankSlip(bankSlip)
                  .build()
            )
            .setSales(listOf())
            .setIsMainRetailer(true)
            .setIsBuybox(false)
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_NAME)
            .setSales(sales)
            .build()
      )

      return offers
   }

   private fun isProductPage(document: Document): Boolean {
      return document.selectFirst(".product-details") != null
   }
}
