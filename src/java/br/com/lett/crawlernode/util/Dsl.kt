package br.com.lett.crawlernode.util

import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import models.Offer
import models.Offers
import models.RatingsReviews
import models.pricing.Pricing

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
  var offers: Offers? = null
  var ratingReviews: RatingsReviews? = null
  private val productBuilder = ProductBuilder()


  inline fun buildOffer(builder: OfferBuilderDsl.() -> Unit) {

  }

  operator fun invoke(): Product = productBuilder
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

@ProductDsl
class OfferBuilderDsl {
  private val offerBuilder = Offer.OfferBuilder()
  var sellerFullName: String? = null
  var internalSellerId: String? = null
  var mainPagePosition: Int? = null
  var sellersPagePosition: Int? = null
  var sellerType: String? = null
  var isBuybox: Boolean? = null
  var sales: List<String>? = null
  var pricing: Pricing? = null
  var isMainRetailer: Boolean? = null
  var useSlugNameAsInternalSellerId = false


  operator fun invoke(): Offer = offerBuilder.setPricing(pricing)
    .setIsBuybox(isBuybox)
    .setSellerFullName(sellerFullName)
    .setSellersPagePosition(sellersPagePosition)
    .setUseSlugNameAsInternalSellerId(useSlugNameAsInternalSellerId)
    .setIsMainRetailer(isMainRetailer)
    .setSales(sales)
    .setSellerType(sellerType)
    .build()

}

inline fun buildProduct(builder: ProductBuilderDsl.() -> Unit): Product {
  val productBuilder = ProductBuilderDsl()

  productBuilder.builder()
  return productBuilder.invoke()
}

fun test() {
  String.let { }
  val p = buildProduct {
    primaryImage = ""

    buildOffer{

    }

  }
}
