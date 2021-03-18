package br.com.lett.crawlernode.crawlers.corecontent.belgium

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.CategoryCollection
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import models.Offer
import models.Offers
import models.pricing.Pricing
import org.json.JSONObject
import java.util.*


/**
 * Date: 18/02/21
 *
 * @author BuSSoLoTTi
 *
 */

class BelgiumDelhaizeCrawler(session: Session) : Crawler(session) {

   init {
      config.fetcher = FetchMode.JAVANET
   }

   companion object {
      const val SELLER_NAME: String = "Delhaize"
   }

   class BelgiumDelhaizeCrawler(session: Session?) : Crawler(session) {
      override fun handleCookiesBeforeFetch() {
         val request = RequestBuilder.create().setUrl("https://www.delhaize.be/").build()
         cookies = dataFetcher.get(session, request).cookies
      }
   }


   override fun fetch(): JSONObject? {

      val pid = CommonMethods.getLast(session.originalURL.split("/"))
      val apiURL = "https://api.delhaize.be/"
      val headers: MutableMap<String, String> = HashMap()
      headers["content-type"] = "application/json"
      val p =
         "{\"operationName\":\"ProductDetails\",\"variables\":{\"productCode\":\"$pid\",\"lang\":\"fr\"},\"query\":\"query ProductDetails(\$anonymousCartCookie: String, \$productCode: String!, \$lang: String!) {\\n  productDetails(anonymousCartCookie: \$anonymousCartCookie, productCode: \$productCode, lang: \$lang) {\\n    additionalLogo\\n    available\\n    availableForPickup\\n    averageRating\\n    badges {\\n      ...ProductBadge\\n      __typename\\n    }\\n    baseOptions\\n    catalogId\\n    catalogVersion\\n    categories {\\n      ...ProductCategory\\n      __typename\\n    }\\n    configurable\\n    classifications {\\n      ...ProductClassification\\n      __typename\\n    }\\n    code\\n    countryFlagUrl\\n    dayPrice\\n    department\\n    description\\n    eanCodes\\n    thumbnailImage\\n    formattedDepartment\\n    freshnessDuration\\n    freshnessDurationTipFormatted\\n    frozen\\n    recyclable\\n    galleryImages {\\n      ...Image\\n      __typename\\n    }\\n    groupedGalleryImages {\\n      ...GroupedImage\\n      __typename\\n    }\\n    wineFoodAssociations {\\n      ...WineFoodAssociation\\n      __typename\\n    }\\n    expertReviewsMedals {\\n      ...ExpertReview\\n      __typename\\n    }\\n    expertReviewsScores {\\n      ...ExpertReview\\n      __typename\\n    }\\n    videoLinks\\n    isReviewEnabled\\n    review {\\n      ...ProductReview\\n      __typename\\n    }\\n    wineRegion {\\n      ...WineRegion\\n      __typename\\n    }\\n    wineProducer {\\n      ...WineProducer\\n      __typename\\n    }\\n    wineExhibitor {\\n      ...WineExhibitor\\n      __typename\\n    }\\n    vintages {\\n      ...Vintage\\n      __typename\\n    }\\n    wsNutriFactData {\\n      ...NutriFact\\n      __typename\\n    }\\n    healthierAlternative {\\n      ...ProductBlockDetails\\n      __typename\\n    }\\n    groupedImages {\\n      ...GroupedImage\\n      __typename\\n    }\\n    hopeId\\n    images {\\n      ...Image\\n      __typename\\n    }\\n    isAvailableByCase\\n    isWine\\n    keywords\\n    limitedAssortment\\n    localizedUrls {\\n      ...LocalizedUrl\\n      __typename\\n    }\\n    delivered\\n    manufacturer\\n    maxOrderQuantity\\n    manufacturerName\\n    manufacturerSubBrandName\\n    miniCartImage {\\n      ...Image\\n      __typename\\n    }\\n    mobileClassificationAttributes {\\n      ...MobileClassificationAttribute\\n      __typename\\n    }\\n    mobileFees {\\n      ...MobileFee\\n      __typename\\n    }\\n    name\\n    nonSEOUrl\\n    numberOfReviews\\n    nutriScoreLetter\\n    nutriScoreLetterImage {\\n      ...Image\\n      __typename\\n    }\\n    onlineExclusive\\n    potentialPromotions {\\n      ...Promotion\\n      __typename\\n    }\\n    price {\\n      ...Price\\n      __typename\\n    }\\n    productProposedPackaging\\n    productProposedPackaging2\\n    purchasable\\n    stock {\\n      ...Stock\\n      __typename\\n    }\\n    summary\\n    totalProductFees\\n    uid\\n    url\\n    previouslyBought\\n    __typename\\n  }\\n}\\n\\nfragment ProductReview on ProductReview {\\n  productRating {\\n    count\\n    value\\n    __typename\\n  }\\n  reviews {\\n    alias\\n    comment\\n    date\\n    headline\\n    rating\\n    message\\n    reasonCode\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment ProductBlockDetails on Product {\\n  available\\n  averageRating\\n  numberOfReviews\\n  manufacturerName\\n  manufacturerSubBrandName\\n  code\\n  freshnessDuration\\n  freshnessDurationTipFormatted\\n  frozen\\n  recyclable\\n  images {\\n    format\\n    imageType\\n    url\\n    __typename\\n  }\\n  maxOrderQuantity\\n  limitedAssortment\\n  name\\n  onlineExclusive\\n  potentialPromotions {\\n    alternativePromotionMessage\\n    code\\n    priceToBurn\\n    promotionType\\n    range\\n    redemptionLevel\\n    toDisplay\\n    description\\n    title\\n    promoBooster\\n    simplePromotionMessage\\n    __typename\\n  }\\n  price {\\n    approximatePriceSymbol\\n    currencySymbol\\n    formattedValue\\n    priceType\\n    supplementaryPriceLabel1\\n    supplementaryPriceLabel2\\n    showStrikethroughPrice\\n    discountedPriceFormatted\\n    unit\\n    unitCode\\n    unitPrice\\n    value\\n    __typename\\n  }\\n  purchasable\\n  productProposedPackaging\\n  productProposedPackaging2\\n  stock {\\n    inStock\\n    inStockBeforeMaxAdvanceOrderingDate\\n    partiallyInStock\\n    availableFromDate\\n    __typename\\n  }\\n  url\\n  previouslyBought\\n  nutriScoreLetter\\n  __typename\\n}\\n\\nfragment ProductBadge on ProductBadge {\\n  code\\n  image {\\n    ...Image\\n    __typename\\n  }\\n  tooltipMessage\\n  __typename\\n}\\n\\nfragment ProductCategory on ProductCategory {\\n  code\\n  catalogId\\n  catalogVersion\\n  name\\n  nameNonLocalized\\n  productCount\\n  sequence\\n  uid\\n  url\\n  __typename\\n}\\n\\nfragment ProductClassification on ProductClassification {\\n  code\\n  features {\\n    code\\n    comparable\\n    featureValues {\\n      value\\n      __typename\\n    }\\n    name\\n    range\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment Image on Image {\\n  altText\\n  format\\n  galleryIndex\\n  imageType\\n  url\\n  __typename\\n}\\n\\nfragment GroupedImage on GroupedImage {\\n  images {\\n    ...Image\\n    __typename\\n  }\\n  index\\n  __typename\\n}\\n\\nfragment LocalizedUrl on LocalizedUrl {\\n  locale\\n  url\\n  __typename\\n}\\n\\nfragment WineFoodAssociation on WineFoodAssociation {\\n  code\\n  title\\n  icon {\\n    ...Image\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment ExpertReview on ExpertReview {\\n  organization\\n  date\\n  score\\n  note\\n  medal\\n  __typename\\n}\\n\\nfragment WineRegion on WineRegion {\\n  code\\n  title\\n  name\\n  country\\n  description\\n  image {\\n    ...Image\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment WineProducer on WineProducer {\\n  id\\n  name\\n  description\\n  street\\n  postalCode\\n  city\\n  state\\n  country\\n  wineTypes\\n  __typename\\n}\\n\\nfragment WineExhibitor on WineExhibitor {\\n  id\\n  name\\n  street\\n  postalCode\\n  city\\n  state\\n  country\\n  __typename\\n}\\n\\nfragment Vintage on Vintage {\\n  product {\\n    ...ProductBlockDetails\\n    __typename\\n  }\\n  year\\n  __typename\\n}\\n\\nfragment NutriFact on NutriFact {\\n  nutrients {\\n    ...NutrientList\\n    __typename\\n  }\\n  allegery {\\n    ...Allergy\\n    __typename\\n  }\\n  ingredients\\n  validLifestyle\\n  otherInfo {\\n    ...OtherInfo\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment MobileClassificationAttribute on MobileClassificationAttribute {\\n  code\\n  value\\n  __typename\\n}\\n\\nfragment MobileFee on MobileFee {\\n  feeName\\n  feeCode\\n  priceData {\\n    ...Price\\n    __typename\\n  }\\n  feeValue\\n  __typename\\n}\\n\\nfragment Price on Price {\\n  approximatePriceSymbol\\n  averageSize\\n  countryCode\\n  currencyIso\\n  currencySymbol\\n  discountedPriceFormatted\\n  formattedValue\\n  fractionValue\\n  intValue\\n  priceType\\n  showStrikethroughPrice\\n  supplementaryPriceLabel1\\n  supplementaryPriceLabel2\\n  unit\\n  unitCode\\n  unitPrice\\n  unitPriceFormatted\\n  value\\n  variableStorePrice\\n  warehouseCode\\n  __typename\\n}\\n\\nfragment Promotion on Promotion {\\n  alternativePromotionMessage\\n  code\\n  promotionType\\n  redemptionLevel\\n  toDisplay\\n  startDate\\n  endDate\\n  description\\n  couldFireMessages\\n  firedMessages\\n  priority\\n  title\\n  toDate\\n  fromDate\\n  promotionClassName\\n  promoStartDate\\n  range\\n  discountPointsPromotion\\n  priceToBurn\\n  promoBooster\\n  simplePromotionMessage\\n  __typename\\n}\\n\\nfragment Stock on Stock {\\n  inStock\\n  inStockBeforeMaxAdvanceOrderingDate\\n  partiallyInStock\\n  availableFromDate\\n  __typename\\n}\\n\\nfragment NutrientList on NutrientList {\\n  nutrients {\\n    id\\n    valueList {\\n      ...OtherInfo\\n      __typename\\n    }\\n    __typename\\n  }\\n  footnote\\n  __typename\\n}\\n\\nfragment OtherInfo on OtherInfo {\\n  value\\n  key\\n  order\\n  __typename\\n}\\n\\nfragment Allergy on Allergy {\\n  id\\n  title\\n  values\\n  __typename\\n}\\n\"}"
      val request = RequestBuilder.create().setUrl(apiURL).setHeaders(headers).setCookies(cookies).setPayload(p).build()
      val response = dataFetcher.post(session, request)
      val json = response.body
      return CrawlerUtils.stringToJson(json)
   }


   override fun extractInformation(json: JSONObject): MutableList<Product> {
      val products: MutableList<Product> = mutableListOf()
      if (isProductPage(json)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + session.originalURL)

         val jsonSku = JSONUtils.getValueRecursive(json, "data.productDetails", JSONObject::class.java, JSONObject())

         val internalId = jsonSku.optString("code")
         val name = jsonSku.optString("name")
         val categories = scrapCategories(jsonSku)

         val images = scrapImages(jsonSku)
         val primaryImage = if (images.size > 0) images.removeAt(0) else null

         val description = scrapDescription(jsonSku)

         val available = scrapAvailable(jsonSku)

         val offers = if (available) scrapOffers(jsonSku) else Offers()

         // Creating the product

         products.add(ProductBuilder.create()
            .setUrl(session.originalURL)
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setDescription(description)
            .setOffers(offers)
            .build())


      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.originalURL)
      }
      return products
   }

   private fun scrapAvailable(jsonsku: JSONObject): Boolean {
      var available = false

      if (jsonsku.has("stock")) {
         val stock = JSONUtils.getJSONValue(jsonsku, "stock")
         if (stock.has("inStock")) {
            available = stock.optBoolean("inStock")
         }
      }
      return available
   }

   private fun scrapDescription(json: JSONObject): String {
      val description: StringBuilder = StringBuilder()
      val nutriFactData = JSONUtils.getJSONValue(json, "wsNutriFactData")
      description.append(nutriFactData.optString("ingredients"))
      val array = JSONUtils.getJSONArrayValue(json, "otherInfo")
      for (obj in array) {
         if (obj is JSONObject) {
            description.append(obj.optString("key"))
            description.append(obj.optString("value"))
         }
      }

      return description.toString()
   }


   private fun scrapImages(json: JSONObject): MutableList<String> {
      val images = mutableListOf<String>()
      val arrayImages = JSONUtils.getJSONArrayValue(json, "galleryImages")
      for (img in arrayImages) {
         if (img is JSONObject)
            if (img.optString("format") == "zoom") {
               val url = img.optString("url")
               images.add(CrawlerUtils.completeUrl(url, "https:", "dhf6qt42idbhy.cloudfront.net"))
            }
      }
      return images
   }

   private fun scrapCategories(json: JSONObject): Collection<String> {
      val category = CategoryCollection()
      category.add(JSONUtils.getValueRecursive(json, "categories.0.name", String::class.java))
      category.add(JSONUtils.getValueRecursive(json, "categories.1.name", String::class.java))
      category.add(JSONUtils.getValueRecursive(json, "categories.2.name", String::class.java))

      return category

   }

   private fun scrapOffers(json: JSONObject): Offers {
      val offers = Offers()

      val spotlightPriceText = JSONUtils.getValueRecursive(json, "price.discountedPriceFormatted", String::class.java, "")
      val spotlightPrice = MathUtils.parseDoubleWithComma(spotlightPriceText)

      val price: Double? = JSONUtils.getValueRecursive(json, "price.value", java.lang.Double::class.java)?.toDouble()

      val bankSlip = spotlightPrice.toBankSlip()
      val creditCards = listOf(Card.MASTERCARD, Card.VISA).toCreditCards(spotlightPrice)

      offers.add(
         Offer.OfferBuilder.create()
            .setPricing(
               Pricing.PricingBuilder.create()
                  .setPriceFrom(price)
                  .setSpotlightPrice(spotlightPrice)
                  .setCreditCards(creditCards)
                  .setBankSlip(bankSlip)
                  .build())
            .setSales(listOf())
            .setIsMainRetailer(true)
            .setIsBuybox(false)
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_NAME)
            .build()
      )
      return offers
   }

   private fun isProductPage(json: JSONObject?): Boolean {
      val a = JSONUtils.getValueRecursive(json, "data.productDetails.code", String::class.java)
      return a != null
   }
}
