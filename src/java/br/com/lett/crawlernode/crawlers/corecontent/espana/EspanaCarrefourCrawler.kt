package br.com.lett.crawlernode.crawlers.corecontent.espana

import br.com.lett.crawlernode.core.models.Card.*
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import models.Offer.OfferBuilder
import models.Offers
import models.pricing.CreditCards
import models.pricing.Pricing.PricingBuilder
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.nodes.Document

class EspanaCarrefourCrawler(session: Session) : Crawler(session) {

  override fun extractInformation(document: Document): List<Product> {
    val products = mutableListOf<Product>()

    val name = document.selectAny("#product-01", ".product-header__name")?.text()

    val split = session.originalURL.split("/")
    val internalId = split[split.size - 2]

    val categories = document.select(".breadcrumb__item").eachText(ignoreIndex = arrayOf(0))

    val description = document.selectFirst(".product-details")?.html()

    val images = document.select(".pics-slider__thumbnail img")?.eachAttr("src")
      ?.map { src -> "\\d.*(?=x_)".toRegex().replace(src, "768") }?.toMutableList()

    val jsonProduct = CrawlerUtils.selectJsonFromHtml(document, "script", "__INITIAL_STATE__=", "};", false, false)
      ?.optJSONObject("pdp")?.optJSONObject("product") ?: JSONObject()

    val productBuilder = ProductBuilder.create()
      .setCategories(categories)
      .setName(name)
      .setDescription(description)
      .setInternalId(internalId)
      .setUrl(session.originalURL)


    products += if (jsonProduct.opt("skus") != null) {
      scrapMutipleSkus(productBuilder, jsonProduct.optJSONArray("skus"), images)
    } else {
      scrapSingleSku(productBuilder, jsonProduct)
    }

    return products
  }

  private fun scrapSingleSku(productBuilder: ProductBuilder, jsonProduct: JSONObject): MutableList<Product> {
    val products = mutableListOf<Product>()
    val jsonOffers = jsonProduct.optJSONObject("offer")

    val images = mutableListOf<String?>()
    for (json in (jsonOffers.optJSONArray("images") ?: JSONArray())) {
      if (json is JSONObject) {
        images.add(json.optString("large"))
      }
    }

    val primary = if (images.size > 0) images.removeAt(0) else null

    val offers: Offers = scrapSingleOffers(jsonOffers)

    products += productBuilder.setInternalId(jsonProduct.optString("product_id"))
      .setOffers(offers)
      .setPrimaryImage(primary)
      .setSecondaryImages(JSONArray(images).toString())
      .build()

    return products

  }

  private fun scrapSingleOffers(jsonOffers: JSONObject): Offers {
    val price = MathUtils.parseDoubleWithComma(jsonOffers.optString("price"))
    val priceFrom = MathUtils.parseDoubleWithComma(jsonOffers.optString("strikethrough_price"))

    val pricing = PricingBuilder.create().setBankSlip(price?.toBankSlip())
      .setSpotlightPrice(price)
      .setPriceFrom(priceFrom)
      .build()

    val offers = Offers()
    offers.add(
      OfferBuilder.create()
        .setPricing(pricing)
        .setUseSlugNameAsInternalSellerId(true)
        .setIsBuybox(false)
        .setSellerFullName("Carrefour")
        .setIsMainRetailer(true)
        .build()
    )

    return offers
  }

  private fun scrapMutipleSkus(productBuilder: ProductBuilder, jsonArray: JSONArray, images: MutableList<String>?): MutableList<Product> {
    val products = mutableListOf<Product>()
    for (skuJson in jsonArray) {
      if (skuJson is JSONObject) {
        val offers = scrapOffers(skuJson.optJSONArray("offers"))

        products += productBuilder
          .setOffers(offers)
          .setPrimaryImage(images?.removeAt(0))
          .setSecondaryImages(JSONArray(images).toString())
          .setEans(listOf(skuJson.optString("ean13")))
          .build()
      }
    }
    return products
  }

  private fun scrapOffers(jsonOffers: JSONArray): Offers {
    val offers = Offers()

    for (sku in jsonOffers) {
      if (sku is JSONObject) {
        val creditCards = CreditCards()

        for (cardInf in sku.optJSONArray("financing_options")) {
          if (cardInf is JSONObject) {
            setOf(VISA, MASTERCARD, AMEX).toCreditCards(instPrice = MathUtils.parseDoubleWithComma(cardInf.optString("first_quota")), instNumber = cardInf.optInt("months"))
              .creditCards.forEach(creditCards::add)
          }
        }
        val price: Double? = MathUtils.parseDoubleWithComma(sku.optString("price"))
        val priceFrom: Double? = MathUtils.parseDoubleWithComma(sku.optString("strikethrough_price"))

        val pricing = PricingBuilder.create().setBankSlip(price?.toBankSlip())
          .setCreditCards(creditCards)
          .setSpotlightPrice(price)
          .setPriceFrom(priceFrom)
          .build()

        val sellerName = sku.optString("seller_name")
        offers.add(
          OfferBuilder.create()
            .setPricing(pricing)
            .setUseSlugNameAsInternalSellerId(true)
            .setIsBuybox(jsonOffers.length() > 1)
            .setSellerFullName(sellerName)
            .setIsMainRetailer("""(?i)^carrefour\s?$""".toRegex().matches(sellerName))
            .build()
        )
      }
    }
    return offers
  }
}