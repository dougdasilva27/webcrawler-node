package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;

import br.com.lett.crawlernode.util.JSONUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public class ColombiaRappiexitobogotaCrawler extends Crawler {

   public ColombiaRappiexitobogotaCrawler(Session session) {
      super(session);
      this.config.setFetcher(FetchMode.APACHE);
   }

   private static final String HOME_PAGE = "https://www.rappi.com.co/";

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   String getStoreId() {
      return "6660303";
   }

   String getHomeDomain() {
      return "grability.rappi.com";
   }

   String getImagePrefix() {
      return "images.rappi.com/products";
   }

   @Override
   protected JSONObject fetch() {
      JSONObject productsInfo = new JSONObject();

      String storeId = getStoreId();

      String productUrl = session.getOriginalURL();
      String productId = null;

      if (productUrl.contains("_")) {
         productId = CommonMethods.getLast(productUrl.split("\\?")[0].split("_"));
      }

      if (productId != null && storeId != null) {
         String token = fetchToken();

         JSONObject data = JSONUtils.stringToJson(fetchProduct(productId, storeId, token));

         JSONArray components = JSONUtils.getValueRecursive(data, "data.components", JSONArray.class);

         if (components != null) {
            for (Object json: components) {
               if (json instanceof JSONObject) {
                  String nameComponents = ((JSONObject) json).optString("name");
                  if (nameComponents.equals("product_information")) {
                     productsInfo = JSONUtils.getJSONValue((JSONObject) json, "resource");
                  }
               }
            }
         }
      }
      return productsInfo;
   }

   private String fetchProduct(String productId, String storeId, String token) {

      String url = "https://services."+getHomeDomain()+"/api/dynamic/context/content";

      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "application/json, text/plain, */*");
      headers.put("language", "pt");
      headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36");
      headers.put("content-type", "application/json");
      headers.put("authorization", token);

      String payload = "{\"state\":{\"product_id\":\""+productId+"\"},\"limit\":100,\"offset\":0,\"context\":\"product_detail\",\"stores\":["+storeId+"]}";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .build();

      return this.dataFetcher.post(session, request).getBody();
   }

   private String fetchToken() {
      String url = "https://services."+getHomeDomain()+"/api/auth/guest_access_token";
      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "application/json, text/plain, */*");
      headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36");
      headers.put("content-type", "application/json");

      String payload = "{\"headers\":{\"normalizedNames\":{},\"lazyUpdate\":null},\"grant_type\":\"guest\"}";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .mustSendContentEncoding(false)
         .build();

      JSONObject json = JSONUtils.stringToJson(this.dataFetcher.post(session, request).getBody());

      String token = json.optString("access_token");
      String tokenType = json.optString("token_type");

      if (tokenType.equals("Bearer")) {
         token = tokenType + " " + token;
      }

      return token;
   }

   @Override
   public List<Product> extractInformation(JSONObject jsonSku) throws Exception {

      List<Product> products = new ArrayList<>();

      JSONObject productJson = JSONUtils.getJSONValue(jsonSku, "product");

      if (isProductPage(productJson)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(productJson);
         String internalPid = crawlInternalPid(productJson);
         String description = crawlDescription(productJson);
         String primaryImage = crawlPrimaryImage(jsonSku);
         String secondaryImages = crawlSecondaryImages(jsonSku, primaryImage);
         CategoryCollection categories = crawlCategories(productJson);
         String name = crawlName(productJson);
         List<String> eans = scrapEan(productJson);
         boolean available = crawlAvailability(productJson);
         Offers offers = available ? scrapOffers(productJson) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setCategories(categories)
            .setDescription(description)
            .setEans(eans)
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   public Offers scrapOffers(JSONObject jsonSku) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();

      try {
         Pricing pricing = scrapPricing(jsonSku);
         List<String> sales = scrapSales(pricing);

         Offer offer = new Offer.OfferBuilder().setSellerFullName("Rappi")
            .setInternalSellerId(jsonSku.optString("store_id", null))
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setPricing(pricing)
            .setIsMainRetailer(true)
            .setSales(sales)
            .build();

         offers.add(offer);
      } catch (Exception e) {
         Logging.printLogWarn(logger, session, "offers error: " + CommonMethods.getStackTraceString(e));
      }

      return offers;
   }

   public static Pricing scrapPricing(JSONObject jsonSku) throws MalformedPricingException {
      Double price = jsonSku.optDouble("price", 0D);
      Double priceFrom = jsonSku.optDouble("real_price", 0D);
      if (price == 0D || price.equals(priceFrom)) {
         price = priceFrom;
         priceFrom = null;
      }
      CreditCards creditCards = scrapCreditCards(price);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(price)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }

   public static CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      Set<Card> cards = Sets.newHashSet(
         Card.VISA,
         Card.MASTERCARD,
         Card.DINERS,
         Card.AMEX,
         Card.ELO,
         Card.SHOP_CARD
      );

      for (Card card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card.toString())
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   public static List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      if (pricing.getPriceFrom() != null && pricing.getPriceFrom() > pricing.getSpotlightPrice()) {
         BigDecimal big = BigDecimal.valueOf(pricing.getPriceFrom() / pricing.getSpotlightPrice() - 1);
         String rounded = big.setScale(2, BigDecimal.ROUND_DOWN).toString();
         sales.add('-' + rounded.replace("0.", "") + '%');
      }

      return sales;
   }

   /*******************************
    * Product page identification *
    *******************************/

   protected boolean isProductPage(JSONObject jsonSku) {
      return jsonSku.length() > 0;
   }

   /*******************
    * General methods *
    *******************/

   protected String crawlInternalId(JSONObject json) {
      String internalId = null;

      if (json.has("id")) {
         internalId = json.get("id").toString();
      }

      return internalId;
   }


   protected String crawlInternalPid(JSONObject json) {
      String internalPid = null;

      if (json.has("product_id")) {
         internalPid = json.getString("product_id");
      }

      return internalPid;
   }

   protected String crawlName(JSONObject json) {
      String name = null;

      if (json.has("name")) {
         name = json.getString("name");
      }

      return name;
   }

   protected boolean crawlAvailability(JSONObject json) {
      return json.has("is_available") && json.getBoolean("is_available");
   }

   protected String crawlPrimaryImage(JSONObject json) {
      String primaryImage;

      JSONArray images = JSONUtils.getJSONArrayValue(json, "images");

      if (!images.isEmpty()) {
         primaryImage = images.getString(0);
      } else {
         primaryImage = JSONUtils.getValueRecursive(json, "product.image", String.class);
      }

      return CrawlerUtils.completeUrl(primaryImage, "https",  getImagePrefix());
   }

   protected String crawlSecondaryImages(JSONObject json, String primaryImage) {
      JSONArray imagesArray = JSONUtils.getJSONArrayValue(json, "images");
      JSONArray resultImages = new JSONArray();

      if (imagesArray.length() > 1) {
         for (int i = 1; i < imagesArray.length(); i++) {

            String imagePath = CrawlerUtils.completeUrl(imagesArray.optString(i), "https",  getImagePrefix());

            if (imagePath != null && !imagePath.equals(primaryImage)) {
               resultImages.put(imagePath);
            }
         }
      }

      return imagesArray.toString();
   }

   protected CategoryCollection crawlCategories(JSONObject json) {
      CategoryCollection categories = new CategoryCollection();

      String category = JSONUtils.getStringValue(json, "category");

      if (!category.isEmpty()) {
         categories.add(category);
      }

      return categories;
   }

   protected String crawlDescription(JSONObject json) {
      StringBuilder description = new StringBuilder();

      if (json.has("description") && json.get("description") instanceof String) {
         description.append(json.getString("description"));
      }
      return description.toString();
   }

   protected List<String> scrapEan(JSONObject jsonSku) {
      List<String> eans = new ArrayList<>();
      String ean;

      if (jsonSku.has("ean")) {
         ean = jsonSku.getString("ean");

         if (ean != null && !ean.isEmpty()) {
            eans.add(ean);
         }
      }

      return eans;
   }

}
