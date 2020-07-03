package br.com.lett.crawlernode.util

import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import models.Offer
import models.Offers
import models.RatingsReviews
import models.pricing.Pricing

fun buildProduct(builder: ProductBuilderDsl.() -> Unit): Product {
  return ProductBuilderDsl().invoke(builder)
}

@DslMarker
annotation class ProductDsl

@ProductDsl
class ProductBuilderDsl {
  lateinit var url: String
  lateinit var internalId: String
  var internalPid: String? = null
  lateinit var name: String
  var categories: MutableCollection<String>? = null
  var primaryImage: String? = null
  var secondaryImages: String? = null
  var description: String? = null
  var stock: Int? = null
  var eans: MutableList<String>? = null
  private var offers = Offers()
  var ratingReviews: RatingsReviews? = null
  private val productBuilder = ProductBuilder()

  operator fun invoke(initializer: ProductBuilderDsl.() -> Unit): Product {
    initializer()
    return productBuilder
      .setUrl(url)
      .setInternalId(internalId)
      .setInternalPid(internalPid)
      .setName(name)
      .setCategories(categories)
      .setPrimaryImage(primaryImage)
      .setSecondaryImages(secondaryImages)
      .setDescription(description)
      .setStock(stock)
      .setEans(eans)
      .setOffers(offers)
      .setRatingReviews(ratingReviews)
      .build()
  }

  fun offer(block: OfferBuilderDsl.() -> Unit) {
    val offerBuilder = OfferBuilderDsl()
    offers.add(offerBuilder.build(block))
  }
}

@ProductDsl
class OfferBuilderDsl {
  private val offerBuilder = Offer.OfferBuilder()

  private var buybox: Boolean? = false
  private var mainRetailer: Boolean = false
  private var slugNameAsInternalSellerId = false
  var sellerFullName: String? = null
  var internalSellerId: String? = null
  var mainPagePosition: Int? = null
  var sellersPagePosition: Int? = null
  var sellerType: String? = null
  var sales: List<String>? = null
  var pricing: Pricing? = null

  val useSlugNameAsInternalSellerId: Boolean
    get() {
      slugNameAsInternalSellerId = true
      return true
    }
  val isBuybox: Boolean
    get() {
      buybox = true
      return true
    }
  val isMainRetailer: Boolean
    get() {
      mainRetailer = true
      return true
    }

  internal fun build(initializer: OfferBuilderDsl.() -> Unit): Offer {
    initializer()

    return offerBuilder.setPricing(pricing)
      .setIsBuybox(buybox)
      .setSellerFullName(sellerFullName)
      .setSellersPagePosition(sellersPagePosition)
      .setUseSlugNameAsInternalSellerId(slugNameAsInternalSellerId)
      .setIsMainRetailer(mainRetailer)
      .setSales(sales)
      .setSellerType(sellerType)
      .build()
  }
}
