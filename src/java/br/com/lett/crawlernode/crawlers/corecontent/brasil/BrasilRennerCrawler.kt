package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrustvoxRatingCrawler
import br.com.lett.crawlernode.util.*
import exceptions.MalformedPricingException
import models.Offer
import models.Offers
import models.RatingsReviews
import models.pricing.CreditCard.CreditCardBuilder
import models.pricing.CreditCards
import models.pricing.Installment
import models.pricing.Installments
import models.pricing.Pricing
import org.json.JSONObject
import org.jsoup.nodes.Document


/**
 * Date: 14/07/20
 *
 * @author Fellype Layunne
 *
 */
class BrasilRennerCrawler(session: Session) : Crawler(session) {

   init {
      config.fetcher = FetchMode.APACHE
   }

   companion object {
      const val SELLER_NAME: String = "Renner"
   }

   override fun fetch(): Document {
      val request = Request.RequestBuilder.create()
         .setUrl(this.session.originalURL)
         .setProxyservice(
            listOf(
               ProxyCollection.LUMINATI_SERVER_BR
            )
         )
         .build()

      val response = dataFetcher.get(session, request)

      return response.body.toDoc() ?: Document(session.originalURL)
   }

   override fun extractInformation(doc: Document): MutableList<Product> {

      val pageJson = CrawlerUtils.selectJsonFromHtml(doc, "#__NEXT_DATA__", null, null, false, false)
      val productJson = pageJson?.optJSONObject("props")?.optJSONObject("pageProps")?.optJSONObject("product")

      if (isProductPage(productJson)) {
         val products = mutableListOf<Product>()

         val internalId = crawlInternalId(productJson)
         val internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=product]", "value")
         val urlBase = "https://www.lojasrenner.com.br"
         val urlPath = pageJson?.optJSONObject("props")?.optJSONObject("pageProps")?.optJSONObject("content")?.optJSONArray("mainContent")?.optJSONObject(0)?.optJSONObject("record")?.optJSONObject("attributes")?.optString("prop.product.url")
         val url = "$urlBase$urlPath?sku=$internalId"
         val baseName = productJson?.optString("displayName") ?: ""
         val currentVariantName = productJson?.optString("variants")?.replace("indefinido", "")?.replace("|", " ") ?: ""
         val name = "$baseName $currentVariantName"
         val categories = CrawlerUtils.crawlCategories(doc, "ul[aria-label='breadcrumb'] li:not(:first-child):not(:last-child) a", true)
         val description = pageJson?.optJSONObject("props")?.optJSONObject("pageProps")?.optJSONObject("content")?.optJSONArray("mainContent")?.optJSONObject(0)?.optJSONObject("record")?.optJSONObject("attributes")?.optString("prop.product.metaDescription")
         val images = productJson?.optJSONArray("mediaSets")?.map { "https:${(it as JSONObject).optString("largeImageUrl")}" }
         val offers = if (productJson?.optBoolean("purchasable") == true) scrapOffers(productJson) else Offers()
         val ratings = scrapRating(internalPid, doc)

         val product = ProductBuilder()
            .setUrl(url)
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(images?.get(0))
            .setSecondaryImages(images?.let { images.subList(1, it.size) })
            .setDescription(description)
            .setOffers(offers)
            .setRatingReviews(ratings)
            .build()

         products addNonNull product

         return products

      }

      return mutableListOf()
   }

   private fun crawlInternalId(productJson: JSONObject?): String? {
      if (this.session.originalURL.contains("?sku=")) {
         val urlSplit = this.session.originalURL.split("?sku=")
         if (urlSplit.size > 1) {
            val internalId = urlSplit[1]
            if (!internalId.equals("")) {
               return internalId
            }
         }
      }

      return productJson?.optString("skuId")
   }

   private fun scrapInstallments(productJson: JSONObject?): Installments {
      val installments = Installments()

      val installmentNumber = productJson?.optInt("installment")
      val installmentPriceFormatted = productJson?.optString("installmentValueFormatted")
      val installmentPrice = MathUtils.parseDoubleWithComma(installmentPriceFormatted)

      if (installmentNumber != null && installmentPrice != null) {
         val installment = Installment.InstallmentBuilder
            .create()
            .setInstallmentNumber(installmentNumber)
            .setInstallmentPrice(installmentPrice)
            .setFinalPrice((installmentNumber.times(installmentPrice)).toDouble().round())
            .build()

         installments.add(installment)
      }

      return installments
   }

   private fun scrapOffers(productJson: JSONObject): Offers {

      val offers = Offers()

      val priceText = productJson.optString("listPriceFormatted")
      val spotlightText = productJson.optString("salePriceFormatted")

      var priceFrom = MathUtils.parseDoubleWithComma(priceText)
      val spotlightPrice = if (productJson.optDouble("percentDiscount") > 0) {
         MathUtils.parseDoubleWithComma(spotlightText)
      } else {
         priceFrom
      }

      spotlightPrice?.let {
         if (spotlightPrice == priceFrom) {
            priceFrom = null
         }

         val sales = mutableListOf<String>()

         sales addNonNull productJson.optDouble("percentDiscount").toString()

         val otherCars =
            listOf(Card.MASTERCARD, Card.VISA, Card.AMEX, Card.DINERS, Card.ELO,
               Card.HIPERCARD).map { card: Card ->
               try {
                  return@map CreditCardBuilder.create()
                     .setBrand(card.toString())
                     .setIsShopCard(false)
                     .setInstallments(
                        scrapInstallments(productJson)
                     )
                     .build()
               } catch (e: MalformedPricingException) {
                  throw RuntimeException(e)
               }
            }

         val creditCards = CreditCards(otherCars)

         offers.add(
            Offer.OfferBuilder.create()
               .setPricing(
                  Pricing.PricingBuilder.create()
                     .setCreditCards(creditCards)
                     .setSpotlightPrice(spotlightPrice)
                     .setBankSlip(spotlightPrice.toBankSlip())
                     .setPriceFrom(priceFrom)
                     .build()
               )
               .setSales(sales)
               .setIsMainRetailer(true)
               .setIsBuybox(false)
               .setUseSlugNameAsInternalSellerId(true)
               .setSellerFullName(SELLER_NAME)
               .build()
         )
      }

      return offers
   }

   private fun isProductPage(productJson: JSONObject?): Boolean {
      return productJson != null
   }

   private fun scrapRating( internalPid: String?, doc: Document?): RatingsReviews? {
      val trustVox = TrustvoxRatingCrawler(session, "110773", logger)
      return trustVox.extractRatingAndReviews(internalPid, doc, dataFetcher)
   }
}
