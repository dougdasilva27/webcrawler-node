package br.com.lett.crawlernode.crawlers.corecontent.belgium

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import com.google.gson.JsonObject
import models.Offer
import models.Offers
import models.pricing.Pricing
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Date: 30/07/20
 *
 * @author Fellype Layunne
 *
 */

class BelgiumDelhaizeCrawler(session: Session) : Crawler(session) {

   init {
      config.fetcher = FetchMode.APACHE;
   }

   companion object {
      const val SELLER_NAME: String = "Delhaize"
   }

   override fun fetch(): Document {

      val Pid = CommonMethods.getLast(session.originalURL.split("/"));

//      val payload = "{\"operationName\":\"ProductDetails\",\"variables\":{\"productCode\":\"" + Pid + "\",\"lang\":\"fr\"},\"query\":\"query ProductDetails(\$anonymousCartCookie: String, \$productCode: Stringu0021, \$lang: Stringu0021) {\\n  productDetails(anonymousCartCookie: \$anonymousCartCookie, productCode: \$productCode, lang: \$lang) {\\n    additionalLogo\\n    available\\n    availableForPickup\\n    averageRating\\n    badges {\\n      ...ProductBadge\\n      __typename\\n    }\\n    baseOptions\\n    catalogId\\n    catalogVersion\\n    categories {\\n      ...ProductCategory\\n      __typename\\n    }\\n    configurable\\n    classifications {\\n      ...ProductClassification\\n      __typename\\n    }\\n    code\\n    countryFlagUrl\\n    dayPrice\\n    department\\n    description\\n    eanCodes\\n    thumbnailImage\\n    formattedDepartment\\n    freshnessDuration\\n    freshnessDurationTipFormatted\\n    frozen\\n    recyclable\\n    galleryImages {\\n      ...Image\\n      __typename\\n    }\\n    groupedGalleryImages {\\n      ...GroupedImage\\n      __typename\\n    }\\n    wineFoodAssociations {\\n      ...WineFoodAssociation\\n      __typename\\n    }\\n    expertReviewsMedals {\\n      ...ExpertReview\\n      __typename\\n    }\\n    expertReviewsScores {\\n      ...ExpertReview\\n      __typename\\n    }\\n    videoLinks\\n    isReviewEnabled\\n    review {\\n      ...ProductReview\\n      __typename\\n    }\\n    wineRegion {\\n      ...WineRegion\\n      __typename\\n    }\\n    wineProducer {\\n      ...WineProducer\\n      __typename\\n    }\\n    wineExhibitor {\\n      ...WineExhibitor\\n      __typename\\n    }\\n    vintages {\\n      ...Vintage\\n      __typename\\n    }\\n    wsNutriFactData {\\n      ...NutriFact\\n      __typename\\n    }\\n    healthierAlternative {\\n      ...ProductBlockDetails\\n      __typename\\n    }\\n    groupedImages {\\n      ...GroupedImage\\n      __typename\\n    }\\n    hopeId\\n    images {\\n      ...Image\\n      __typename\\n    }\\n    isAvailableByCase\\n    isWine\\n    keywords\\n    limitedAssortment\\n    localizedUrls {\\n      ...LocalizedUrl\\n      __typename\\n    }\\n    delivered\\n    manufacturer\\n    maxOrderQuantity\\n    manufacturerName\\n    manufacturerSubBrandName\\n    miniCartImage {\\n      ...Image\\n      __typename\\n    }\\n    mobileClassificationAttributes {\\n      ...MobileClassificationAttribute\\n      __typename\\n    }\\n    mobileFees {\\n      ...MobileFee\\n      __typename\\n    }\\n    name\\n    nonSEOUrl\\n    numberOfReviews\\n    nutriScoreLetter\\n    nutriScoreLetterImage {\\n      ...Image\\n      __typename\\n    }\\n    onlineExclusive\\n    potentialPromotions {\\n      ...Promotion\\n      __typename\\n    }\\n    price {\\n      ...Price\\n      __typename\\n    }\\n    productProposedPackaging\\n    productProposedPackaging2\\n    purchasable\\n    stock {\\n      ...Stock\\n      __typename\\n    }\\n    summary\\n    totalProductFees\\n    uid\\n    url\\n    previouslyBought\\n    __typename\\n  }\\n}\\n\\nfragment ProductReview on ProductReview {\\n  productRating {\\n    count\\n    value\\n    __typename\\n  }\\n  reviews {\\n    alias\\n    comment\\n    date\\n    headline\\n    rating\\n    message\\n    reasonCode\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment ProductBlockDetails on Product {\\n  available\\n  averageRating\\n  numberOfReviews\\n  manufacturerName\\n  manufacturerSubBrandName\\n  code\\n  freshnessDuration\\n  freshnessDurationTipFormatted\\n  frozen\\n  recyclable\\n  images {\\n    format\\n    imageType\\n    url\\n    __typename\\n  }\\n  maxOrderQuantity\\n  limitedAssortment\\n  name\\n  onlineExclusive\\n  potentialPromotions {\\n    alternativePromotionMessage\\n    code\\n    priceToBurn\\n    promotionType\\n    range\\n    redemptionLevel\\n    toDisplay\\n    description\\n    title\\n    promoBooster\\n    simplePromotionMessage\\n    __typename\\n  }\\n  price {\\n    approximatePriceSymbol\\n    currencySymbol\\n    formattedValue\\n    priceType\\n    supplementaryPriceLabel1\\n    supplementaryPriceLabel2\\n    showStrikethroughPrice\\n    discountedPriceFormatted\\n    unit\\n    unitCode\\n    unitPrice\\n    value\\n    __typename\\n  }\\n  purchasable\\n  productProposedPackaging\\n  productProposedPackaging2\\n  stock {\\n    inStock\\n    inStockBeforeMaxAdvanceOrderingDate\\n    partiallyInStock\\n    availableFromDate\\n    __typename\\n  }\\n  url\\n  previouslyBought\\n  nutriScoreLetter\\n  __typename\\n}\\n\\nfragment ProductBadge on ProductBadge {\\n  code\\n  image {\\n    ...Image\\n    __typename\\n  }\\n  tooltipMessage\\n  __typename\\n}\\n\\nfragment ProductCategory on ProductCategory {\\n  code\\n  catalogId\\n  catalogVersion\\n  name\\n  nameNonLocalized\\n  productCount\\n  sequence\\n  uid\\n  url\\n  __typename\\n}\\n\\nfragment ProductClassification on ProductClassification {\\n  code\\n  features {\\n    code\\n    comparable\\n    featureValues {\\n      value\\n      __typename\\n    }\\n    name\\n    range\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment Image on Image {\\n  altText\\n  format\\n  galleryIndex\\n  imageType\\n  url\\n  __typename\\n}\\n\\nfragment GroupedImage on GroupedImage {\\n  images {\\n    ...Image\\n    __typename\\n  }\\n  index\\n  __typename\\n}\\n\\nfragment LocalizedUrl on LocalizedUrl {\\n  locale\\n  url\\n  __typename\\n}\\n\\nfragment WineFoodAssociation on WineFoodAssociation {\\n  code\\n  title\\n  icon {\\n    ...Image\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment ExpertReview on ExpertReview {\\n  organization\\n  date\\n  score\\n  note\\n  medal\\n  __typename\\n}\\n\\nfragment WineRegion on WineRegion {\\n  code\\n  title\\n  name\\n  country\\n  description\\n  image {\\n    ...Image\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment WineProducer on WineProducer {\\n  id\\n  name\\n  description\\n  street\\n  postalCode\\n  city\\n  state\\n  country\\n  wineTypes\\n  __typename\\n}\\n\\nfragment WineExhibitor on WineExhibitor {\\n  id\\n  name\\n  street\\n  postalCode\\n  city\\n  state\\n  country\\n  __typename\\n}\\n\\nfragment Vintage on Vintage {\\n  product {\\n    ...ProductBlockDetails\\n    __typename\\n  }\\n  year\\n  __typename\\n}\\n\\nfragment NutriFact on NutriFact {\\n  nutrients {\\n    ...NutrientList\\n    __typename\\n  }\\n  allegery {\\n    ...Allergy\\n    __typename\\n  }\\n  ingredients\\n  validLifestyle\\n  otherInfo {\\n    ...OtherInfo\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment MobileClassificationAttribute on MobileClassificationAttribute {\\n  code\\n  value\\n  __typename\\n}\\n\\nfragment MobileFee on MobileFee {\\n  feeName\\n  feeCode\\n  priceData {\\n    ...Price\\n    __typename\\n  }\\n  feeValue\\n  __typename\\n}\\n\\nfragment Price on Price {\\n  approximatePriceSymbol\\n  averageSize\\n  countryCode\\n  currencyIso\\n  currencySymbol\\n  discountedPriceFormatted\\n  formattedValue\\n  fractionValue\\n  intValue\\n  priceType\\n  showStrikethroughPrice\\n  supplementaryPriceLabel1\\n  supplementaryPriceLabel2\\n  unit\\n  unitCode\\n  unitPrice\\n  unitPriceFormatted\\n  value\\n  variableStorePrice\\n  warehouseCode\\n  __typename\\n}\\n\\nfragment Promotion on Promotion {\\n  alternativePromotionMessage\\n  code\\n  promotionType\\n  redemptionLevel\\n  toDisplay\\n  startDate\\n  endDate\\n  description\\n  couldFireMessages\\n  firedMessages\\n  priority\\n  title\\n  toDate\\n  fromDate\\n  promotionClassName\\n  promoStartDate\\n  range\\n  discountPointsPromotion\\n  priceToBurn\\n  promoBooster\\n  simplePromotionMessage\\n  __typename\\n}\\n\\nfragment Stock on Stock {\\n  inStock\\n  inStockBeforeMaxAdvanceOrderingDate\\n  partiallyInStock\\n  availableFromDate\\n  __typename\\n}\\n\\nfragment NutrientList on NutrientList {\\n  nutrients {\\n    id\\n    valueList {\\n      ...OtherInfo\\n      __typename\\n    }\\n    __typename\\n  }\\n  footnote\\n  __typename\\n}\\n\\nfragment OtherInfo on OtherInfo {\\n  value\\n  key\\n  order\\n  __typename\\n}\\n\\nfragment Allergy on Allergy {\\n  id\\n  title\\n  values\\n  __typename\\n}\\n\"}"
      val payload = "{\"operationName\":\"ProductDetails\",\"variables\":{\"productCode\":\"F2011072500565000000\",\"lang\":\"fr\"},\"query\":\"query ProductDetails(\$anonymousCartCookie: String, \$productCode: String!, \$lang: String!) {\\n  productDetails(anonymousCartCookie: \$anonymousCartCookie, productCode: \$productCode, lang: \$lang) {\\n    additionalLogo\\n    available\\n    availableForPickup\\n    averageRating\\n    badges {\\n      ...ProductBadge\\n      __typename\\n    }\\n    baseOptions\\n    catalogId\\n    catalogVersion\\n    categories {\\n      ...ProductCategory\\n      __typename\\n    }\\n    configurable\\n    classifications {\\n      ...ProductClassification\\n      __typename\\n    }\\n    code\\n    countryFlagUrl\\n    dayPrice\\n    department\\n    description\\n    eanCodes\\n    thumbnailImage\\n    formattedDepartment\\n    freshnessDuration\\n    freshnessDurationTipFormatted\\n    frozen\\n    recyclable\\n    galleryImages {\\n      ...Image\\n      __typename\\n    }\\n    groupedGalleryImages {\\n      ...GroupedImage\\n      __typename\\n    }\\n    wineFoodAssociations {\\n      ...WineFoodAssociation\\n      __typename\\n    }\\n    expertReviewsMedals {\\n      ...ExpertReview\\n      __typename\\n    }\\n    expertReviewsScores {\\n      ...ExpertReview\\n      __typename\\n    }\\n    videoLinks\\n    isReviewEnabled\\n    review {\\n      ...ProductReview\\n      __typename\\n    }\\n    wineRegion {\\n      ...WineRegion\\n      __typename\\n    }\\n    wineProducer {\\n      ...WineProducer\\n      __typename\\n    }\\n    wineExhibitor {\\n      ...WineExhibitor\\n      __typename\\n    }\\n    vintages {\\n      ...Vintage\\n      __typename\\n    }\\n    wsNutriFactData {\\n      ...NutriFact\\n      __typename\\n    }\\n    healthierAlternative {\\n      ...ProductBlockDetails\\n      __typename\\n    }\\n    groupedImages {\\n      ...GroupedImage\\n      __typename\\n    }\\n    hopeId\\n    images {\\n      ...Image\\n      __typename\\n    }\\n    isAvailableByCase\\n    isWine\\n    keywords\\n    limitedAssortment\\n    localizedUrls {\\n      ...LocalizedUrl\\n      __typename\\n    }\\n    delivered\\n    manufacturer\\n    maxOrderQuantity\\n    manufacturerName\\n    manufacturerSubBrandName\\n    miniCartImage {\\n      ...Image\\n      __typename\\n    }\\n    mobileClassificationAttributes {\\n      ...MobileClassificationAttribute\\n      __typename\\n    }\\n    mobileFees {\\n      ...MobileFee\\n      __typename\\n    }\\n    name\\n    nonSEOUrl\\n    numberOfReviews\\n    nutriScoreLetter\\n    nutriScoreLetterImage {\\n      ...Image\\n      __typename\\n    }\\n    onlineExclusive\\n    potentialPromotions {\\n      ...Promotion\\n      __typename\\n    }\\n    price {\\n      ...Price\\n      __typename\\n    }\\n    productProposedPackaging\\n    productProposedPackaging2\\n    purchasable\\n    stock {\\n      ...Stock\\n      __typename\\n    }\\n    summary\\n    totalProductFees\\n    uid\\n    url\\n    previouslyBought\\n    __typename\\n  }\\n}\\n\\nfragment ProductReview on ProductReview {\\n  productRating {\\n    count\\n    value\\n    __typename\\n  }\\n  reviews {\\n    alias\\n    comment\\n    date\\n    headline\\n    rating\\n    message\\n    reasonCode\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment ProductBlockDetails on Product {\\n  available\\n  averageRating\\n  numberOfReviews\\n  manufacturerName\\n  manufacturerSubBrandName\\n  code\\n  freshnessDuration\\n  freshnessDurationTipFormatted\\n  frozen\\n  recyclable\\n  images {\\n    format\\n    imageType\\n    url\\n    __typename\\n  }\\n  maxOrderQuantity\\n  limitedAssortment\\n  name\\n  onlineExclusive\\n  potentialPromotions {\\n    alternativePromotionMessage\\n    code\\n    priceToBurn\\n    promotionType\\n    range\\n    redemptionLevel\\n    toDisplay\\n    description\\n    title\\n    promoBooster\\n    simplePromotionMessage\\n    __typename\\n  }\\n  price {\\n    approximatePriceSymbol\\n    currencySymbol\\n    formattedValue\\n    priceType\\n    supplementaryPriceLabel1\\n    supplementaryPriceLabel2\\n    showStrikethroughPrice\\n    discountedPriceFormatted\\n    unit\\n    unitCode\\n    unitPrice\\n    value\\n    __typename\\n  }\\n  purchasable\\n  productProposedPackaging\\n  productProposedPackaging2\\n  stock {\\n    inStock\\n    inStockBeforeMaxAdvanceOrderingDate\\n    partiallyInStock\\n    availableFromDate\\n    __typename\\n  }\\n  url\\n  previouslyBought\\n  nutriScoreLetter\\n  __typename\\n}\\n\\nfragment ProductBadge on ProductBadge {\\n  code\\n  image {\\n    ...Image\\n    __typename\\n  }\\n  tooltipMessage\\n  __typename\\n}\\n\\nfragment ProductCategory on ProductCategory {\\n  code\\n  catalogId\\n  catalogVersion\\n  name\\n  nameNonLocalized\\n  productCount\\n  sequence\\n  uid\\n  url\\n  __typename\\n}\\n\\nfragment ProductClassification on ProductClassification {\\n  code\\n  features {\\n    code\\n    comparable\\n    featureValues {\\n      value\\n      __typename\\n    }\\n    name\\n    range\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment Image on Image {\\n  altText\\n  format\\n  galleryIndex\\n  imageType\\n  url\\n  __typename\\n}\\n\\nfragment GroupedImage on GroupedImage {\\n  images {\\n    ...Image\\n    __typename\\n  }\\n  index\\n  __typename\\n}\\n\\nfragment LocalizedUrl on LocalizedUrl {\\n  locale\\n  url\\n  __typename\\n}\\n\\nfragment WineFoodAssociation on WineFoodAssociation {\\n  code\\n  title\\n  icon {\\n    ...Image\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment ExpertReview on ExpertReview {\\n  organization\\n  date\\n  score\\n  note\\n  medal\\n  __typename\\n}\\n\\nfragment WineRegion on WineRegion {\\n  code\\n  title\\n  name\\n  country\\n  description\\n  image {\\n    ...Image\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment WineProducer on WineProducer {\\n  id\\n  name\\n  description\\n  street\\n  postalCode\\n  city\\n  state\\n  country\\n  wineTypes\\n  __typename\\n}\\n\\nfragment WineExhibitor on WineExhibitor {\\n  id\\n  name\\n  street\\n  postalCode\\n  city\\n  state\\n  country\\n  __typename\\n}\\n\\nfragment Vintage on Vintage {\\n  product {\\n    ...ProductBlockDetails\\n    __typename\\n  }\\n  year\\n  __typename\\n}\\n\\nfragment NutriFact on NutriFact {\\n  nutrients {\\n    ...NutrientList\\n    __typename\\n  }\\n  allegery {\\n    ...Allergy\\n    __typename\\n  }\\n  ingredients\\n  validLifestyle\\n  otherInfo {\\n    ...OtherInfo\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment MobileClassificationAttribute on MobileClassificationAttribute {\\n  code\\n  value\\n  __typename\\n}\\n\\nfragment MobileFee on MobileFee {\\n  feeName\\n  feeCode\\n  priceData {\\n    ...Price\\n    __typename\\n  }\\n  feeValue\\n  __typename\\n}\\n\\nfragment Price on Price {\\n  approximatePriceSymbol\\n  averageSize\\n  countryCode\\n  currencyIso\\n  currencySymbol\\n  discountedPriceFormatted\\n  formattedValue\\n  fractionValue\\n  intValue\\n  priceType\\n  showStrikethroughPrice\\n  supplementaryPriceLabel1\\n  supplementaryPriceLabel2\\n  unit\\n  unitCode\\n  unitPrice\\n  unitPriceFormatted\\n  value\\n  variableStorePrice\\n  warehouseCode\\n  __typename\\n}\\n\\nfragment Promotion on Promotion {\\n  alternativePromotionMessage\\n  code\\n  promotionType\\n  redemptionLevel\\n  toDisplay\\n  startDate\\n  endDate\\n  description\\n  couldFireMessages\\n  firedMessages\\n  priority\\n  title\\n  toDate\\n  fromDate\\n  promotionClassName\\n  promoStartDate\\n  range\\n  discountPointsPromotion\\n  priceToBurn\\n  promoBooster\\n  simplePromotionMessage\\n  __typename\\n}\\n\\nfragment Stock on Stock {\\n  inStock\\n  inStockBeforeMaxAdvanceOrderingDate\\n  partiallyInStock\\n  availableFromDate\\n  __typename\\n}\\n\\nfragment NutrientList on NutrientList {\\n  nutrients {\\n    id\\n    valueList {\\n      ...OtherInfo\\n      __typename\\n    }\\n    __typename\\n  }\\n  footnote\\n  __typename\\n}\\n\\nfragment OtherInfo on OtherInfo {\\n  value\\n  key\\n  order\\n  __typename\\n}\\n\\nfragment Allergy on Allergy {\\n  id\\n  title\\n  values\\n  __typename\\n}\\n\"}"
      val headers = mutableMapOf<String,String>();
      headers["content-type"] = "application/json"
      headers["cookie"] = "bm_sz=74BCC5B8A79015EE44CF42BA49BB36B2~YAAQdqIkvW5mn2p3AQAAYqVjkQrnA1DJnbMIQz6C2/yIy5qlxe2/HmYx8KTzIN5FRkuZRHkMhfzwCHL6CuQ4yLNHELXn3Csl9jsc3j/XlPUqeXJ+5GpEjJvz7/s766Nv/7Ab6cJgdTZae5gwHyBDdX4Mwz88araVR8snS8QJ5vvoeFpe7Z1roGjNR2MmQ4xZYA==; rxVisitor=16130519630539M82H83NJV86CEKKO9V22OR5HFVHAD36; at_check=true; deviceSessionId=uU61s9MIkL1N3ZwQx2bgj; _cs_c=1; _abck=3FCA29C73EF8DB7A494C9FDC80D86E18~0~YAAQdqIkvYFmn2p3AQAAnK1jkQWlvwzp6ydr1v6vXx13rFgstindil5431m6gJhZZbk47oQkdfXKVQ5wnXifEa+AWTuQHxWh1Cu/SONWiu6uTa/zZqlYfYUqqMZFPelQQ1QGkdhlGtRVvlJXCXgZWiPcZqNN99HLsGamkbjmekqulowXjmMdGBYNoygnWsOLwh/KCDUGkYyxPXmQ3N29iXPrv3jVp02EFjdnZ+iU9PPDMR1S2Mz5Hyg/oLVLVW5NfZny0XMsxHVwWpELKuVpAPGw1vw69SA2q96tAszQkWT2++8bIDVC5SRHbjB/eHxpB1kVN9W/+nWeSG/B1u2bCqLl/GwDLWQ=~-1~-1~-1; ak_bmsc=6794DA7ED3B17E34681E7C450F07C10BBD24A276CC2900003A38256013D5B725~pl4fS/+7FNOGh9I7K3YkjFEMPXeZH6KpReyJ1vy3MbadAlKApmir3gmclRp7gU76MLfSdvxQMdpaztM+C6bmaYZvz6ZczZrkR8Yp6yCo/YRhMeH5bGI2+u5hp3IrB9HyqCODEjPfnFn2RS7SPHUoZ2ADtSWbfs/EXbU0O+1yNoT8WS6xmZQG4ulDEco/1Qvtu9MJw1st48aGy+C7zHZad72lfjGW88J72CZZdmp60csxTBAzhv0UKq/nm7/I+rI1nZ; grocery-ccatc=vo2ZgRL6-rRsrUTzgTe1CLYU5bQ; dtCookie=v_4_srv_3_sn_242F4FFF7217195EED08D8D1A8439CE9_perc_100000_ol_0_mul_1_app-3Ab0bf94df3db180f6_1; adobeujs-optin=%7B%22aam%22%3Afalse%2C%22adcloud%22%3Afalse%2C%22aa%22%3Atrue%2C%22campaign%22%3Afalse%2C%22ecid%22%3Atrue%2C%22livefyre%22%3Afalse%2C%22target%22%3Atrue%2C%22mediaaa%22%3Afalse%7D; AMCVS_2A6E210654E74B040A4C98A7%40AdobeOrg=1; _gcl_au=1.1.258524044.1613051968; _ga=GA1.2.603195312.1613051968; _gid=GA1.2.142954696.1613051968; s_fid=1263A6DC07E72158-37E8873FE4ACE43A; s_cc=true; AMCV_2A6E210654E74B040A4C98A7%40AdobeOrg=-637568504%7CMCMID%7C36696668639172165991553799532861728957%7CMCAAMLH-1613656777%7C4%7CMCAAMB-1613656777%7CRKhpRz8krg2tLO6pguXWp5olkAcUniQYPHaMWWgdJ3xzPWQmdj0y%7CMCOPTOUT-1613059177s%7CNONE%7CMCAID%7CNONE%7CvVersion%7C5.1.1%7CMCIDTS%7C18670; _fbp=fb.1.1613051979268.64678867; _pin_unauth=dWlkPVpUUmxZVFZpT0dVdFpUZGlNeTAwTW1OaExUZzVOV0l0Wm1JNU0yRTRZV00xTXpNMQ; sto__session=1613052770211; sto__vuid=f4a5faef36a95d940bf2cf2f8ee87fbf; sto__count=1; s_ppn=homepage%3Aindex; AWSALB=hg62PSUZ2Ruorw1f0mdE1MzI6/3oCAwRVKGFCwwez0u4djk+FbGT9/TA0ZyMZyEB3ns76tnr4XeuzpnbFVGK8TJR5NCZ51W6cyPtf/gwxyYaCZopY9kdxrkqc/bM; dtSa=-; mbox=PC#206ed5e8ebd14a1eab5d486a2a34f260.34_0#1676299283|session#2fbbad01e58945799284c308ccd6af87#1613056342; s_sq=%5B%5BB%5D%5D; bm_sv=0387E9EDA97B2BBFA80826A74F25439A~vijTamtqkS8zn5a5RXYXYhkfR4WH1teoUR9P17cOd01ged/Ik7ZQ6uZbEM6CiIcJZ5FKU2DklazmokbDyVxTx06qaw0VrT48qZDp79RnIHPBdnQYAJnFjwYtTld5uQVuzpTOW+Thmc7WOEGgLqs/1QgCC+dOGMQmsXmANcTHa/E=; dtLatC=36; _uetsid=6253c6106c7111eb9bf2356f37a3fbd6; _uetvid=625425706c7111ebbbcab97614c9465c; _cs_id=8bce5b47-14ec-a03c-c0a7-2b8896162297.1613051964.1.1613054593.1613051964.1.1647215964157.Lax.0; _cs_s=10.0; rxvt=1613056393775|1613051963055; dtPC=3\$254593205_685h3vCJUHHLBSPFNFHMCTWRFQRCUFVHLTFRSH-0e7"

      println(payload)

      val response = dataFetcher.post(
         session, RequestBuilder.create()
            .setHeaders(headers)
            .setUrl("https://api.delhaize.be/")
            .setPayload(payload)
            .setProxyservice(
               listOf(
                  ProxyCollection.BUY,
                  ProxyCollection.INFATICA_RESIDENTIAL_BR,
                  ProxyCollection.BONANZA_BELGIUM
               )
            )
            .build()
      )

      return Jsoup.parse(response.body);
   }

   override fun extractInformation(json: JSONObject?): MutableList<Product> {
      return super.extractInformation(json)
   }

   private fun scrapName(doc: Document): String {

      var name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-details .page-title", false) + " " +
         CrawlerUtils.scrapStringSimpleInfo(doc, ".page-title-info", false)

      if (doc.selectFirst(".ProductDetails.product-details") == null) {
         name += " " + CrawlerUtils.scrapStringSimpleInfo(doc, ".test-qty-property", false)
      }

      return name

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
