package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import models.AdvancedRatingReview
import models.Offer.OfferBuilder
import models.Offers
import models.RatingsReviews
import models.pricing.Pricing.PricingBuilder
import org.json.JSONArray
import org.jsoup.nodes.Document

class BrasilNutricaoatevoceCrawler(session: Session?) : Crawler(session) {

   val sellerName: String = "Nutrição Até Você";

   override fun extractInformation(document: Document): MutableList<Product> {
      val products = mutableListOf<Product>()

      document.selectFirst(".product-info-main")?.let {

         val name = CrawlerUtils.scrapStringSimpleInfo(document, "h1.page-title", false)
         val internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, ".product-add-form [data-product-sku]", "data-product-sku")
         val internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, "input[name=product]", "value")
         val categories = CrawlerUtils.crawlCategories(document, ".breadcrumbs .items .item:not(.home):not(.product)")

         val offers = scrapOffers(document)
         val imagesArray = CrawlerUtils.crawlArrayImagesFromScriptMagento(document)
         val primaryImage = scrapPrimaryImage(imagesArray, document)
         val secondaryImages = CrawlerUtils.scrapSecondaryImagesMagento(imagesArray, primaryImage)
         val description = CrawlerUtils.scrapSimpleDescription(document, mutableListOf(".product.info .product.data.items"))
         val rating = scrapRating(document)

         // Creating the product
         products += ProductBuilder.create()
            .setUrl(session.originalURL)
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setOffers(offers)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setRatingReviews(rating)
            .build()
      }

      return products
   }

   private fun scrapPrimaryImage(images: JSONArray, doc: Document): String? {
      var image: String? = "";
      if (!images.isEmpty) {
         image = images.getString(0)
      }
      return image
   }

   private fun scrapOffers(doc: Document): Offers {
      val offers = Offers()
      var price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-add-form .tier-price-amount .price, .product-add-form [id*=product-price-] .price", null, true, ',', session)
      var priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-add-form span[id*=old-price] .price", null, true, ',', session)
      var priceSale = CrawlerUtils.scrapDoublePriceFromHtml(doc,".catalog-only span[data-price-amount]",null,true,',',session)

      if(priceSale!=null&&priceFrom==null){
         priceFrom =  price
         price = priceSale
      }

      price?.let {
         if (price == priceFrom) {
            priceFrom = null
         }

         val sales = mutableListOf<String>()
         sales addNonNull CrawlerUtils.scrapStringSimpleInfo(doc, ".product-add-form .tier-price-description", false)
         val creditCards = setOf(Card.VISA, Card.MASTERCARD, Card.ELO, Card.AMEX).toCreditCards(price)

         offers.add(
            OfferBuilder.create()
               .setPricing(
                  PricingBuilder.create()
                     .setCreditCards(creditCards)
                     .setBankSlip(price.toBankSlip())
                     .setSpotlightPrice(price)
                     .setPriceFrom(priceFrom)
                     .build()
               )
               .setSales(sales)
               .setIsMainRetailer(true)
               .setIsBuybox(false)
               .setUseSlugNameAsInternalSellerId(true)
               .setSellerFullName(sellerName)
               .build()
         )
      }

      return offers
   }

   private fun scrapRating(doc: Document): RatingsReviews {
      var rating = RatingsReviews()

      val starMap: MutableMap<Int, Int> = HashMap()

      val evaluations = doc.select(".review-items li.review-item span[itemprop=ratingValue]")
      if (!evaluations.isEmpty()) {
         for (evaluation in evaluations) {
            val percent = CrawlerUtils.scrapIntegerFromHtml(evaluation, null, true, 0)

            if (percent >= 1) {
               val star: Int = MathUtils.normalizeNoDecimalPlaces(5 * (percent / 100.0)).toInt()

               if (starMap.contains(star)) {
                  starMap.put(star, starMap.getValue(star) + 1)
               } else {
                  starMap.put(star, 1)
               }
            }
         }

         val advanced = AdvancedRatingReview.Builder().allStars(starMap).build()
         val average = CrawlerUtils.extractRatingAverageFromAdvancedRatingReview(advanced)
         val count = CrawlerUtils.extractReviwsNumberOfAdvancedRatingReview(advanced)

         rating.setAdvancedRatingReview(advanced)
         rating.setAverageOverallRating(average)
         rating.setTotalRating(count);
         rating.setTotalWrittenReviews(count);
      }

      return rating;
   }
}
