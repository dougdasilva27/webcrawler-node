package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.exceptions.MalformedUrlException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import com.google.common.net.HttpHeaders;
import exceptions.MalformedPricingException;
import models.Offer;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.CreditCard;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;


//essa classe não deve ser criada na scraper class
public abstract class RappiCrawler extends Crawler {

   protected static final Pattern URL_PATH_PATTERN = Pattern.compile(".*/([0-9][^a-zA-Z]*)_([0-9][^a-zA-Z]*)");

   public RappiCrawler(Session session) {
      super(session);
      this.config.setFetcher(FetchMode.APACHE);
   }

   protected String getStoreId() {
      return session.getOptions().optString("storeId");
   }

   protected String storeId() {
      if (session.getOptions().optBoolean("newUnification", false)) {
         return session.getOptions().optString("storeId");
      } else {
         return getStoreId();
      }
   }

   abstract protected String getHomeDomain();

   abstract protected String getImagePrefix();

   abstract protected String getUrlPrefix();

   abstract protected String getHomeCountry();

   protected boolean newUnification = session.getOptions().optBoolean("newUnification", false);

   @Override
   public void handleCookiesBeforeFetch() {
      BasicClientCookie cookie = new BasicClientCookie("currentLocation", "eyJhZGRyZXNzIjoiMDQxODItMDAxLCBBdmVuaWRhIE1hcmdpbmFsIERpcmVpdGEgQW5jaGlldGEgLSBKYXJkaW0gU2FudGEgQ3J1eiAoU2Fjb21hKSwgU+NvIFBhdWxvIC0gU3RhdGUgb2YgU+NvIFBhdWxvLCBCcmF6aWwiLCJzZWNvbmRhcnlMYWJlbCI6IkF2ZW5pZGEgTWFyZ2luYWwgRGlyZWl0YSBBbmNoaWV0YSAtIEphcmRpbSBTYW50YSBDcnV6IChTYWNvbWEpLCBT428gUGF1bG8gLSBTdGF0ZSBvZiBT428gUGF1bG8sIEJyYXppbCIsImRpc3RhbmNlSW5LbXMiOjExLjgsInBsYWNlSWQiOiJDaElKdGY3a3AyWmJ6cFFSZHN0NEFuYWt5S0EiLCJwbGFjZUluZm9ybWF0aW9uIjpudWxsLCJzb3VyY2UiOiJnb29nbGUiLCJpZCI6MSwiZGVzY3JpcHRpb24iOiIiLCJsYXQiOi0yMy42NDk5NTcsImxuZyI6LTQ2LjU4NDA0MDUsImNvdW50cnkiOiJCcmF6aWwiLCJhY3RpdmUiOnRydWV9");
      cookie.setDomain(".www." + getHomeDomain());
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   //   @Override
//   protected JSONObject fetch() {
//      JSONObject productsInfo = new JSONObject();
//
//      String storeId = storeId();
//
//      String productUrl = session.getOriginalURL();
//
//
//      if (newUnification) {
//         if (!checkUrl(productUrl)) {
//            throw new MalformedUrlException("Formato da URL incorreto");
//         } else if (!productUrl.contains(storeId)) {
//            throw new MalformedUrlException("URL não corresponde ao market");
//         }
//      }
//
//
//      String productId = null;
//
//      if (productUrl.contains("_")) {
//         productId = CommonMethods.getLast(productUrl.split("\\?")[0].split("_"));
//      }
//
//      if (productId != null && storeId != null) {
//         String token = fetchToken();
//
//         JSONObject data = fetchProduct(productId, storeId, token);
//
//         JSONArray components = JSONUtils.getValueRecursive(data, "data.components", JSONArray.class);
//
//         if (components != null) {
//            for (Object json : components) {
//               if (json instanceof JSONObject) {
//                  String nameComponents = ((JSONObject) json).optString("name");
//                  if (nameComponents.contains("product_information")) {
//                     productsInfo = JSONUtils.getJSONValue((JSONObject) json, "resource");
//                  }
//               }
//            }
//         }
//      }
//      return productsInfo;
//   }

   private static boolean checkUrl(String productUrl) {
      try {
         URL url = new URL(productUrl);
         boolean checkHost = url.getHost().contains("www.rappi.com");
         boolean checkPath = URL_PATH_PATTERN.matcher(url.getPath()).matches();

         return checkHost && checkPath;
      } catch (MalformedURLException e) {
         return false;
      }
   }

   protected JSONObject fetchProduct(Document doc, String storeId) {
      JSONObject productsInfo = new JSONObject();

      JSONObject pageJson = CrawlerUtils.selectJsonFromHtml(doc, "#__NEXT_DATA__", null, null, false, false);

      JSONArray stores = JSONUtils.getValueRecursive(pageJson, "props.pageProps.master_product_detail_response.data.components.0.resource.product.stores", JSONArray.class, new JSONArray());

      for (int i = 0; i < stores.length(); i++) {
         JSONObject store = stores.optJSONObject(i);
            if (store != null && storeId.equals(store.optString("store_id"))) {
               productsInfo = store.optJSONObject("product");
               break;
            }
      }

      return productsInfo;
   }

   protected String fetchToken() {
      String url = "https://services." + getHomeDomain() + "/api/auth/guest_access_token";

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
   public List<Product> extractInformation(Document doc) throws Exception {

      List<Product> products = new ArrayList<>();

      String storeId = storeId();
//      String productUrl = session.getOriginalURL();

      JSONObject productJson = fetchProduct(doc, storeId);


      if (isProductPage(productJson)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = productJson.optString("master_product_id");
         String internalId = productJson.optString("product_id");
         String description = productJson.optString("description");
         JSONArray images = productJson.optJSONArray("images");
         String primaryImage = images.length() > 0 ? CrawlerUtils.completeUrl(images.optString(0), "https", getImagePrefix()) : null;
         List<String> secondaryImages = crawlSecondaryImages(images);
         CategoryCollection categories = crawlCategories(productJson);
         String name = productJson.optString("name");
         List<String> eans = List.of(productJson.optString("ean"));
         boolean available = productJson.optBoolean("in_stock");
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

   public Offers scrapOffers(JSONObject productJson) {
      Offers offers = new Offers();

      try {
         Pricing pricing = scrapPricing(productJson);
         List<String> sales = scrapSales(pricing);

         Offer offer = new OfferBuilder().setSellerFullName("Rappi")
            .setInternalSellerId(productJson.optString("store_type", null))
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setPricing(pricing)
            .setIsMainRetailer(true)
            .setSales(sales)
            .build();

         offers.add(offer);
      } catch (Exception e) {
         Logging.printLogWarn(logger, session, "offers error: " + session.getOriginalURL());
      }

      return offers;
   }

   public static Pricing scrapPricing(JSONObject productJson) throws MalformedPricingException {
      Double priceFrom = productJson.optDouble("real_price", 0D);
      Double price = productJson.optDouble("balance_price", 0D);
      if (price == 0D || price.equals(priceFrom)) {
         price = priceFrom;
         priceFrom = null;
      }
      CreditCards creditCards = scrapCreditCards(price);

      return PricingBuilder.create()
         .setSpotlightPrice(price)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }

   public static CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(InstallmentBuilder.create()
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

   protected boolean isProductPage(JSONObject productJson) {
//      if (newUnification) {
//         return jsonSku.length() > 0 && session.getOriginalURL().contains(storeId());
//      } else {
//         return jsonSku.length() > 0;
//      }
      return productJson.length() > 0;
   }

   /*******************
    * General methods *
    *******************/

   public String getUrl(JSONObject productJson) {

      String idToUrl = productJson.optString("id");

      return idToUrl != null ? getHomeCountry() + getUrlPrefix() + idToUrl : null;

   }

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
      StringBuilder nameComplet = new StringBuilder();

      if (json.has("name")) {
         nameComplet.append(json.optString("name")).append(" ");
      }
      if (json.has("quantity") && json.optInt("quantity") != 0) {
         nameComplet.append(json.optString("quantity")).append(" ");
         if (json.has("unit_type")) {
            nameComplet.append(json.optString("unit_type"));
         }

      }

      return nameComplet.toString();
   }

   protected boolean crawlAvailability(JSONObject json) {
      return json.has("in_stock") && json.getBoolean("in_stock");
   }

   protected String crawlPrimaryImage(JSONObject json) {
      String primaryImage = null;

      JSONObject product = json.optJSONObject("product");
      if (product != null) {
         String imageId = product.optString("image");
         primaryImage = CrawlerUtils.completeUrl(imageId, "https://", getImagePrefix());
      }

      return primaryImage;
   }

   protected List<String> crawlSecondaryImages(JSONArray images) {
      ArrayList<String> resultImages = new ArrayList<>();

      if (images.length() > 1) {
         for (int i = 1; i < images.length(); i++) {
            String imagePath = CrawlerUtils.completeUrl(images.optString(i), "https", getImagePrefix());
            if (imagePath != null) {
               resultImages.add(imagePath);
            }
         }
      }

      return resultImages;
   }

   protected CategoryCollection crawlCategories(JSONObject productJson) {
      CategoryCollection categories = new CategoryCollection();

      String category = JSONUtils.getStringValue(productJson, "category_name");

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
}
