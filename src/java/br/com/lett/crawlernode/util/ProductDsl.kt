@file:Suppress("unused")

package br.com.lett.crawlernode.util

import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import models.Offer
import models.Offers
import models.RatingsReviews
import models.pricing.*
import models.pricing.BankSlip.BankSlipBuilder
import models.pricing.Pricing.PricingBuilder
import org.slf4j.LoggerFactory

fun product(builder: ProductBuilderDsl.() -> Unit): Product {
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
  var categories: List<String> = mutableListOf()
  var primaryImage: String? = null
  var secondaryImages: String? = null
  var description: String? = null
  var stock: Int? = null
  var eans: MutableList<String> = mutableListOf()
  private var offers = Offers()
  var ratingReviews: RatingsReviews? = null
  private val productBuilder = ProductBuilder()
  private val logger = LoggerFactory.getLogger(this::class.java)

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
     try {
        offers.add(offerBuilder.build(block))
     }catch (e: Exception){
        logger.warn("Not possible to build offer", e)
     }
  }
}

@ProductDsl
class OfferBuilderDsl {
  private val offerBuilder = Offer.OfferBuilder()

  private var buybox: Boolean? = false
  private var mainRetailer: Boolean = false
  private var slugNameAsInternalSellerId = false
  private var pricingDsl = PricingBuilderDsl()
  var sellerFullName: String? = null
  var internalSellerId: String? = null
  var mainPagePosition: Int? = null
  var sellersPagePosition: Int? = null
  var sellerType: String? = null
  var sales: MutableList<String> = mutableListOf()

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

  fun pricing(block: PricingBuilderDsl.() -> Unit) {
    pricingDsl.block()
  }

  internal fun build(initializer: OfferBuilderDsl.() -> Unit): Offer {
    initializer()

    return offerBuilder.setPricing(pricingDsl.build())
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

@ProductDsl
class PricingBuilderDsl {
  private val pricingBuilder = PricingBuilder()
  var creditCards = CreditCards()
  var priceFrom: Double? = null
  var spotlightPrice: Double? = null
  var bankSlip: BankSlip? = null

  fun bankSlip(block: BankslipBuilderDsl.() -> Unit) {
    val bankSlipDsl = BankslipBuilderDsl()
    bankSlipDsl.block()
    bankSlip = bankSlipDsl.build()
  }

  fun creditCard(block: CreditCardBuilderDsl.() -> Unit) {
    val creditCardDsl = CreditCardBuilderDsl()
    creditCardDsl.block()
    creditCards.add(creditCardDsl.build())
  }

  internal fun build(): Pricing {
    return pricingBuilder.setBankSlip(bankSlip)
      .setCreditCards(creditCards)
      .setPriceFrom(priceFrom)
      .setSpotlightPrice(spotlightPrice)
      .build()
  }
}

@ProductDsl
class CreditCardBuilderDsl {
  private val creditCardBuilder = CreditCard.CreditCardBuilder()
  var brand: String? = null
  var shopCard: Boolean = false
  var installments: Installments = Installments()
  val isShopCard: Boolean
    get() {
      shopCard = true
      return true
    }

  internal fun build(): CreditCard {
    return creditCardBuilder.setBrand(brand)
      .setInstallments(installments)
      .setIsShopCard(shopCard)
      .build()
  }
}

@ProductDsl
class BankslipBuilderDsl {
  private val bankSlipBuilder = BankSlipBuilder()
  var finalPrice: Double? = null
  var pageDiscount: Double? = null

  internal fun build(): BankSlip {
    return bankSlipBuilder.setFinalPrice(finalPrice)
      .setOnPageDiscount(pageDiscount).build()
  }
}
