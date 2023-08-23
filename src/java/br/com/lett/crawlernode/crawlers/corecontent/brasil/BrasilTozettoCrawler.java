package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilTozettoCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "tozetto";
   private final String storeId = session.getOptions().optString("storeId");

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.ELO.toString(), Card.HIPERCARD.toString(), Card.ALELO.toString());

   public BrasilTozettoCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSON);
   }

   @Override
   protected Response fetchResponse() {
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");
      headers.put("Origin", "https://online.tozetto.com.br");
      headers.put("Referer", "https://online.tozetto.com.br/");

      String productId = getProductId(session.getOriginalURL());
      JSONObject payload = new JSONObject();
      payload.put("operationName", "ProductDetailQuery");
      payload.put("query", "fragment ProductPromotionFragment on PublicViewerProductPromotion {\n  promotionPrice\n  discountType\n  percentageValue\n  gift\n  buy\n  progressiveDiscount {\n    asFrom\n    gift\n    __typename\n  }\n  isGift\n  promotion {\n    active\n    id\n    name\n    endDate\n    startDate\n    displayExclusivePriceOnline\n    groupProducts\n    progressiveDiscountType\n    availableForExpressDelivery\n    enableLimitPerCustomer\n    qtyToGift\n    updatedAt\n    type\n    benefitType\n    __typename\n  }\n  giftOfThisProduct {\n    productId\n    name\n    image {\n      url\n      thumborized\n      __typename\n    }\n    normalPrice\n    promotionPrice\n    gift\n    __typename\n  }\n  __typename\n}\n\nfragment ProductConfigurationFragment on PublicViewerProductConfiguration {\n  id\n  nextToExpirationDate\n  highlighted\n  enableMaturitySelection\n  displayExpirationDate\n  expirationDate\n  __typename\n}\n\nfragment SimilarDetails on PublicViewerProduct {\n  id\n  name\n  description\n  content\n  saleUnit\n  contentUnit\n  type\n  slug\n  tags\n  brand {\n    id\n    name\n    __typename\n  }\n  image {\n    url\n    thumborized\n    __typename\n  }\n  imagesGallery {\n    name\n    url\n    __typename\n  }\n  productPromotion(storeId: $storeId) {\n    ...ProductPromotionFragment\n    __typename\n  }\n  productConfiguration(storeId: $storeId) {\n    ...ProductConfigurationFragment\n    __typename\n  }\n  quantity(storeId: $storeId) {\n    min\n    max\n    maxPromotion\n    fraction\n    inStock\n    sellByWeightAndUnit\n    __typename\n  }\n  pricing(storeId: $storeId) {\n    id\n    promotion\n    price\n    promotionalPrice\n    orderBumpPrice\n    __typename\n  }\n  personas(storeId: $storeId) {\n    personaPrice\n    personaId\n    __typename\n  }\n  level1Category(storeId: $storeId) {\n    id\n    name\n    slug\n    __typename\n  }\n  level2Category(storeId: $storeId) {\n    id\n    name\n    slug\n    parent {\n      id\n      name\n      slug\n      __typename\n    }\n    __typename\n  }\n  level3Category(storeId: $storeId) {\n    id\n    name\n    slug\n    level2Category: parent {\n      level1Category: parent {\n        id\n        name\n        slug\n        __typename\n      }\n      id\n      name\n      slug\n      __typename\n    }\n    __typename\n  }\n  __typename\n}\n\nfragment ProductDetails on PublicViewerProduct {\n  id\n  name\n  description\n  content\n  saleUnit\n  contentUnit\n  type\n  slug\n  tags\n  brand {\n    id\n    name\n    __typename\n  }\n  image {\n    url\n    thumborized(width: 321, height: 321, fitIn: true)\n    thumbLarge: thumborized(width: 321, height: 321, fitIn: true)\n    __typename\n  }\n  imagesGallery {\n    name\n    url\n    thumborized(width: 321, height: 321, fitIn: true)\n    thumbLarge: thumborized(width: 321, height: 321)\n    __typename\n  }\n  productPromotion(storeId: $storeId) {\n    ...ProductPromotionFragment\n    __typename\n  }\n  productConfiguration(storeId: $storeId) {\n    ...ProductConfigurationFragment\n    __typename\n  }\n  quantity(storeId: $storeId) {\n    min\n    max\n    maxPromotion\n    fraction\n    inStock\n    sellByWeightAndUnit\n    __typename\n  }\n  pricing(storeId: $storeId) {\n    id\n    promotion\n    price\n    promotionalPrice\n    orderBumpPrice\n    __typename\n  }\n  personas(storeId: $storeId) {\n    personaPrice\n    personaId\n    __typename\n  }\n  level1Category(storeId: $storeId) {\n    id\n    name\n    slug\n    __typename\n  }\n  level2Category(storeId: $storeId) {\n    id\n    name\n    slug\n    description\n    slug\n    image {\n      url\n      name\n      thumborized\n      __typename\n    }\n    parent {\n      id\n      name\n      slug\n      __typename\n    }\n    __typename\n  }\n  level3Category(storeId: $storeId) {\n    id\n    name\n    slug\n    level2Category: parent {\n      level1Category: parent {\n        id\n        name\n        slug\n        __typename\n      }\n      id\n      name\n      slug\n      __typename\n    }\n    __typename\n  }\n  __typename\n}\n\nfragment BoughtTogether on PublicViewerProduct {\n  id\n  name\n  description\n  content\n  saleUnit\n  contentUnit\n  type\n  slug\n  tags\n  brand {\n    id\n    name\n    __typename\n  }\n  image {\n    url\n    thumborized(width: 321, height: 321, fitIn: true)\n    __typename\n  }\n  imagesGallery {\n    name\n    url\n    thumborized(width: 321, height: 321, fitIn: true)\n    __typename\n  }\n  productPromotion(storeId: $storeId) {\n    ...ProductPromotionFragment\n    __typename\n  }\n  quantity(storeId: $storeId) {\n    min\n    max\n    maxPromotion\n    fraction\n    inStock\n    sellByWeightAndUnit\n    __typename\n  }\n  pricing(storeId: $storeId) {\n    id\n    promotion\n    price\n    promotionalPrice\n    orderBumpPrice\n    __typename\n  }\n  personas(storeId: $storeId) {\n    personaPrice\n    personaId\n    __typename\n  }\n  level1Category(storeId: $storeId) {\n    id\n    name\n    slug\n    __typename\n  }\n  level2Category(storeId: $storeId) {\n    id\n    name\n    slug\n    parent {\n      id\n      name\n      slug\n      __typename\n    }\n    __typename\n  }\n  level3Category(storeId: $storeId) {\n    id\n    name\n    slug\n    level2Category: parent {\n      level1Category: parent {\n        id\n        name\n        slug\n        __typename\n      }\n      id\n      name\n      slug\n      __typename\n    }\n    __typename\n  }\n  productConfiguration(storeId: $storeId) {\n    ...ProductConfigurationFragment\n    __typename\n  }\n  __typename\n}\n\nfragment UpsellingProduct on PublicViewerProduct {\n  id\n  name\n  description\n  content\n  saleUnit\n  contentUnit\n  type\n  slug\n  tags\n  brand {\n    id\n    name\n    __typename\n  }\n  image {\n    url\n    thumborized(width: 321, height: 321, fitIn: true)\n    thumbLarge: thumborized(width: 321, height: 321, fitIn: true)\n    __typename\n  }\n  imagesGallery {\n    name\n    url\n    thumborized(width: 321, height: 321, fitIn: true)\n    thumbLarge: thumborized(width: 321, height: 321)\n    __typename\n  }\n  productPromotion(storeId: $storeId) {\n    ...ProductPromotionFragment\n    __typename\n  }\n  productConfiguration(storeId: $storeId) {\n    ...ProductConfigurationFragment\n    __typename\n  }\n  quantity(storeId: $storeId) {\n    min\n    max\n    maxPromotion\n    fraction\n    inStock\n    sellByWeightAndUnit\n    __typename\n  }\n  pricing(storeId: $storeId) {\n    id\n    promotion\n    price\n    promotionalPrice\n    orderBumpPrice\n    __typename\n  }\n  personas(storeId: $storeId) {\n    personaPrice\n    personaId\n    __typename\n  }\n  level1Category(storeId: $storeId) {\n    id\n    name\n    slug\n    __typename\n  }\n  level2Category(storeId: $storeId) {\n    id\n    name\n    slug\n    description\n    slug\n    image {\n      url\n      name\n      thumborized\n      __typename\n    }\n    parent {\n      id\n      name\n      slug\n      __typename\n    }\n    __typename\n  }\n  level3Category(storeId: $storeId) {\n    id\n    name\n    slug\n    level2Category: parent {\n      level1Category: parent {\n        id\n        name\n        slug\n        __typename\n      }\n      id\n      name\n      slug\n      __typename\n    }\n    __typename\n  }\n  __typename\n}\n\nquery ProductDetailQuery($storeId: ID!, $productId: ID!) {\n  publicViewer(storeId: $storeId) {\n    id\n    product(id: $productId, storeId: $storeId) {\n      ...ProductDetails\n      similar(storeId: $storeId) {\n        ...SimilarDetails\n        __typename\n      }\n      boughtTogether(storeId: $storeId) {\n        ...BoughtTogether\n        __typename\n      }\n      upsellingProduct(storeId: $storeId) {\n        ...UpsellingProduct\n        __typename\n      }\n      __typename\n    }\n    __typename\n  }\n}");
      JSONObject variables = new JSONObject();
      variables.put("productId", productId);
      variables.put("storeId", storeId);
      payload.put("variables", variables);


      Request request = Request.RequestBuilder.create()
         .setUrl("https://api.online.tozetto.com.br/graphql")
         .setHeaders(headers)
         .setPayload(payload.toString())
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.LUMINATI_SERVER_BR))
         .build();

      Response response = CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher(), false);
      return response;

      //return CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher(), false);
   }

   private String getProductId (String url){
      String regex = "/produtos/(\\d+)/";
      Pattern pattern = Pattern.compile(regex);
      Matcher matcher = pattern.matcher(url);
      if (matcher.find()) {
         return matcher.group(1);
      }
      else{
         Logging.printLogDebug(logger, session, "productId not found!");
         return "";
      }
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      List<Product> products = new ArrayList<>();
      JSONObject productJson = JSONUtils.getValueRecursive(json, "data.publicViewer.product", JSONObject.class, new JSONObject());

      if (productJson != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = JSONUtils.getStringValue(productJson, "id");
         String name = JSONUtils.getStringValue(productJson, "name");
         CategoryCollection categories = getCategories(productJson);
         String primaryImage = JSONUtils.getValueRecursive(productJson, "image.thumborized", String.class);
         String description = JSONUtils.getStringValue(productJson, "description");
         Integer stock = JSONUtils.getValueRecursive(productJson, "quantity.inStock", Integer.class);
         boolean availableToBuy = stock != null && stock != 0;
         Offers offers = availableToBuy ? scrapOffer(productJson) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }
   private  CategoryCollection getCategories(JSONObject productJson){
      CategoryCollection categories = new CategoryCollection();
      String categoryLvl1 = JSONUtils.getValueRecursive(productJson, "level1Category.name", String.class);
      String categoryLvl2 = JSONUtils.getValueRecursive(productJson, "level2Category.name", String.class);
      String categoryLvl3 = JSONUtils.getValueRecursive(productJson, "level3Category.name", String.class);
      if (categoryLvl1 != null){
         categories.add(categoryLvl1);
      }
      if (categoryLvl2 != null){
         categories.add(categoryLvl2);
      }
      if (categoryLvl3 != null){
         categories.add(categoryLvl3);
      }
      return categories;
   }

   private Offers scrapOffer(JSONObject productJson) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productJson);
      List<String> sales = new ArrayList<>();
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private Pricing scrapPricing(JSONObject productJson) throws MalformedPricingException {
      Boolean isOnSale = JSONUtils.getValueRecursive(productJson, "pricing.promotion", Boolean.class);
      Double spotlightPrice = isOnSale ? JSONUtils.getValueRecursive(productJson, "pricing.promotionalPrice", Double.class) : JSONUtils.getValueRecursive(productJson, "pricing.price", Double.class);
      if (spotlightPrice == null){
         Integer intSpotlightPrice =  isOnSale ? JSONUtils.getValueRecursive(productJson, "pricing.promotionalPrice", Integer.class) : JSONUtils.getValueRecursive(productJson, "pricing.price", Integer.class);
         spotlightPrice = (double) intSpotlightPrice;
      }
      Double priceFrom = isOnSale ? JSONUtils.getValueRecursive(productJson, "pricing.price", Double.class) : null;
      if (isOnSale && priceFrom == null){
         Integer intPriceFrom = JSONUtils.getValueRecursive(productJson, "pricing.price", Integer.class);
         priceFrom = (double) intPriceFrom;
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }
   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

}
