package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public abstract class RappiCrawler extends Crawler {

   public RappiCrawler(Session session) {
      super(session);
      this.config.setFetcher(FetchMode.APACHE);
   }

   private final String grammatureRegex = "(\\d+[.,]?\\d*\\s?)(ml|l|g|gr|mg|kg)";
   private final String quantityRegex = "(\\d+[.,]?\\d*\\s?)(und)";

   protected final String RANDOM_DEVICE_ID = UUID.randomUUID().toString();

   protected String getStoreId() {
      return session.getOptions().optString("storeId");
   }

   protected boolean checkNewUnification() {
      return session.getOptions().optBoolean("newUnification", false);
   }

   private String getCurrentLocation() {
      return session.getOptions().optString("currentLocation");
   }

   abstract protected String getHomeDomain();

   abstract protected String getImagePrefix();

   abstract protected String getUrlPrefix();

   abstract protected String getHomeCountry();

   abstract protected String getMarketBaseUrl();

   @Override
   public void handleCookiesBeforeFetch() {
      BasicClientCookie cookie = new BasicClientCookie("currentLocation", getCurrentLocation());
      cookie.setDomain(".www." + getHomeDomain());
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   protected JSONObject fetchProduct(Document doc, String storeId) throws MalformedURLException {
      JSONObject productsInfo = new JSONObject();

      JSONObject pageJson = CrawlerUtils.selectJsonFromHtml(doc, "#__NEXT_DATA__", null, null, false, false);

      JSONObject productJson = JSONUtils.getValueRecursive(pageJson, "props.pageProps.master_product_detail_response.data.components.0.resource.product", JSONObject.class, new JSONObject());
      if (productJson.isEmpty()) {
         JSONObject fallback = JSONUtils.getValueRecursive(pageJson, "props.pageProps.fallback", JSONObject.class, new JSONObject());
         if (!fallback.isEmpty()) {
            Iterator<String> keys = fallback.keys();
            while(keys.hasNext()) {
               String key = keys.next();
               if (fallback.get(key) instanceof JSONObject) {
                  productJson = JSONUtils.getValueRecursive(fallback.get(key), "master_product_detail_response.data.components.0.resource.product", JSONObject.class, new JSONObject());
               }

               if (productJson.isEmpty()) { // product fallback
                  productJson = JSONUtils.getValueRecursive(fallback.get(key), "primaryStore.product", JSONObject.class, new JSONObject());
               }

               if (productJson.isEmpty()) { //product unavailable fallback
                  productJson = fallback.optJSONObject(key);
               }
            }
         }
      }

      JSONArray stores = productJson.optJSONArray("stores");

      if (stores != null) {
         // trying to find the current store in the store listing on product page
         for (int i = 0; i < stores.length(); i++) {
            JSONObject store = stores.optJSONObject(i);
            if (store != null && storeId != null && storeId.equals(store.optString("store_id"))) {
               productsInfo = store.optJSONObject("product");
               break;
            }
         }
      }

      // if the current store is not in the listing, then try to get the product data from API by product id
      if (productsInfo.isEmpty()) {
         boolean isOldUrl = checkOldUrl(this.session.getOriginalURL());
         // if the url is in the new url format, then the crawler will try to get the product data from the rappi search page
         // the crawler will search by product title and check by image UUID
         String productFoundInternalId = isOldUrl ? CommonMethods.getLast(this.session.getOriginalURL().split("_")) : getProductIdFromRanking(productJson);

         if (productFoundInternalId != null) {
            String token = fetchToken();
            productsInfo = fetchProductApi(productFoundInternalId, token);
         }
      }

      return productsInfo;
   }

   protected boolean checkOldUrl(String productUrl) throws MalformedURLException {
      URL url = new URL(productUrl);
      final Pattern urlPathPattern = Pattern.compile(".*/([0-9][^a-zA-Z]*)_([0-9][^a-zA-Z]*)");

      boolean checkHost = url.getHost().contains(getHomeDomain());
      boolean checkPath = urlPathPattern.matcher(url.getPath()).matches();

      return checkHost && checkPath;
   }

   protected String getProductIdFromRanking(JSONObject productJson) {
      String productName = productJson.optString("name");
      String productNameEncoded = productName != null ? StringUtils.stripAccents(productName.replace("%", "")).replace(" ", "%20") : null;
      String url = getMarketBaseUrl() + getStoreId() + "/s?term=" + productNameEncoded;
      Request request = Request.RequestBuilder.create()
         .setCookies(this.cookies)
         .setUrl(url)
         .setFollowRedirects(true)
         .build();

      Response response = CrawlerUtils.retryRequest(request, session, new FetcherDataFetcher(), true);

      Document docRanking = Jsoup.parse(response.getBody());
      JSONObject rankingPageJson = CrawlerUtils.selectJsonFromHtml(docRanking, "#__NEXT_DATA__", null, null, false, false);
      JSONArray searchProducts = JSONUtils.getValueRecursive(rankingPageJson, "props.pageProps.products", JSONArray.class, new JSONArray());

      if (searchProducts.isEmpty()) {
         JSONObject fallback = JSONUtils.getValueRecursive(rankingPageJson, "props.pageProps.fallback", JSONObject.class, new JSONObject());
         if (!fallback.isEmpty()) {
            Iterator<String> keys = fallback.keys();
            while(keys.hasNext()) {
               String key = keys.next();
               if (fallback.get(key) instanceof JSONObject) {
                  searchProducts = JSONUtils.getValueRecursive(fallback.get(key), "products", JSONArray.class, new JSONArray());
               }
            }
         }
      }

      String productFoundInternalId = null;
      if (searchProducts.length() > 0) {
         for (int i = 0; i < searchProducts.length(); i++) {
            JSONObject searchProduct = searchProducts.getJSONObject(i);
            boolean productEquals = checkProductEquals(productJson, searchProduct);
            if (productEquals) {
               productFoundInternalId = searchProduct.optString("product_id");
               break;
            }
         }
      }

      return productFoundInternalId;
   }

   protected boolean checkProductEquals(JSONObject productJson, JSONObject searchProduct) {
      String productId = productJson.optString("product_id");
      String productName = productJson.optString("name");
      String productDescription = crawlDescription(productJson);
      String productImage = crawlImage(productJson);

      String idProductSearch = searchProduct.optString("product_id");
      String nameProductSearch = searchProduct.optString("name");
      String descriptionProductSearch = searchProduct.optString("description");
      String imageProductSearch = searchProduct.optString("image");

      if  ((imageProductSearch != null && imageProductSearch.equals(productImage)) || (productId != null && productId.equals(idProductSearch))) {
         return true;
      }
      
      return productName != null && productDescription != null && productName.equals(nameProductSearch) && productDescription.equals(descriptionProductSearch);
   }

   private String crawlImage(JSONObject productJson) {
      String productImage = productJson.optString("image");
      if (productImage != null && productImage.equals("NO-IMAGE")) {
         JSONArray stores = JSONUtils.getJSONArrayValue(productJson, "stores");
            for (int i = 0; i < stores.length(); i++) {
               JSONObject store = stores.optJSONObject(i);
               if (store != null) {
                  JSONObject productInfo = store.optJSONObject("product");
                  if (productInfo != null && !productInfo.optString("image").equals("NO-IMAGE")) {
                     productImage = productInfo.optString("image");
                     break;
                  }
               }
            }
      }

      return productImage;
   }

   private String crawlDescription(JSONObject productJson) {
      String productDescription = productJson.optString("description");
      if (productDescription != null && productDescription.equals("")) {
         JSONArray stores = JSONUtils.getJSONArrayValue(productJson, "stores");
         for (int i = 0; i < stores.length(); i++) {
            JSONObject store = stores.optJSONObject(i);
            if (store != null) {
               JSONObject productInfo = store.optJSONObject("product");
               if (productInfo != null && !productInfo.optString("description").equals("")) {
                  productDescription = productInfo.optString("description");
                  break;
               }
            }
         }
      }

      return productDescription;
   }

   protected String fetchPassportToken() {
      String url = "https://services." + getHomeDomain() + "/api/rocket/v2/guest/passport/";

      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "application/json, text/plain, */*");
      headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36");
      headers.put("content-type", "application/json");
      headers.put("deviceid", RANDOM_DEVICE_ID);
      headers.put("needAppsFlyerId", "false");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(List.of(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY
         ))
         .build();

      JSONObject json = JSONUtils.stringToJson(CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher(), true).getBody());

      return json.optString("token");
   }

   protected String fetchToken() {
      String url = "https://services." + getHomeDomain() + "/api/rocket/v2/guest";

      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "application/json, text/plain, */*");
      headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36");
      headers.put("content-type", "application/json");
      headers.put("deviceId", RANDOM_DEVICE_ID);
      headers.put("x-guest-api-key", fetchPassportToken());

      String payload = "";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .mustSendContentEncoding(false)
         .setProxyservice(List.of(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY
         ))
         .build();

      JSONObject json = JSONUtils.stringToJson(CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher(), false).getBody());

      String token = json.optString("access_token");
      String tokenType = json.optString("token_type");

      if (tokenType.equals("Bearer")) {
         token = tokenType + " " + token;
      }

      return token;
   }

   protected JSONObject fetchProductApi(String productId, String token) {
      String url = "https://services." + getHomeDomain() + "/api/web-gateway/web/dynamic/context/content/";

      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "application/json, text/plain, */*");
      headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36");
      headers.put("content-type", "application/json");
      headers.put("authorization", token);
      headers.put("deviceid", RANDOM_DEVICE_ID);
      headers.put("app-version", "web_v1.140.9");

      String payload = "{\"state\":{\"product_id\":\"" + productId + "\",\"lat\":\"1\",\"lng\":\"1\"},\"stores\":[" + getStoreId() + "],\"context\":\"product_detail\",\"limit\":10,\"offset\":0}";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .setProxyservice(List.of(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY
         ))
         .build();

      String body = this.dataFetcher.post(session, request).getBody();

      JSONObject jsonObject = CrawlerUtils.stringToJSONObject(body);

      return JSONUtils.getValueRecursive(jsonObject, "data.components.0.resource.product", ".", JSONObject.class, new JSONObject());
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {

      List<Product> products = new ArrayList<>();

      String storeId = getStoreId();

      JSONObject productJson = fetchProduct(doc, storeId);


      if (isProductPage(productJson)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = productJson.optString("master_product_id");
         String internalId = checkNewUnification() ? productJson.optString("product_id") : productJson.optString("id");
         String description = productJson.optString("description");
         JSONArray images = productJson.optJSONArray("images");
         String primaryImage = images.length() > 0 ? CrawlerUtils.completeUrl(images.optString(0), "https", getImagePrefix()) : null;
         List<String> secondaryImages = crawlSecondaryImages(images);
         CategoryCollection categories = crawlCategories(productJson);
         String name = scrapName(productJson);
         List<String> eans = List.of(productJson.optString("ean"));
         boolean available = productJson.optBoolean("in_stock");
         Offers offers = available ? scrapOffers(productJson) : new Offers();
         String url = getHomeCountry() + getUrlPrefix() + getStoreId() + "_" + productJson.optString("product_id", "");

         Product product = ProductBuilder.create()
            .setUrl(url)
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

   private boolean stringHasGrammature(String string) {
      Pattern pattern = Pattern.compile(grammatureRegex, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
      Matcher matcher = pattern.matcher(string);

      return matcher.find();
   }

   private String extractGrammature(String string) {
      Pattern pattern = Pattern.compile(grammatureRegex, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
      final Matcher matcher = pattern.matcher(string);

      return matcher.find() ? matcher.group(0) : "";
   }

   private boolean stringHasQuantity(String string) {
      Pattern pattern = Pattern.compile(quantityRegex, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
      Matcher matcher = pattern.matcher(string);

      return matcher.find();
   }

   private String extractQuantity(String string) {
      Pattern pattern = Pattern.compile(quantityRegex, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
      final Matcher matcher = pattern.matcher(string);

      return matcher.find() ? matcher.group(0) : "";
   }

   private String scrapName(JSONObject productJson) {
      String name = productJson.optString("name");
      String presentation = productJson.optString("presentation");
      String description = productJson.optString("description");

      if (!stringHasGrammature(name) && stringHasGrammature(presentation)) {
         name += " " + presentation;
      } else if (!stringHasGrammature(name) && stringHasGrammature(description)) {
         name += " " + extractGrammature(description);
      }

      if (!stringHasQuantity(name) && stringHasQuantity(presentation)) {
         name += " " + extractQuantity(presentation);
      } else if (!stringHasQuantity(name) && stringHasQuantity(description)) {
         name += " " + extractQuantity(description);
      }

      return name;
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

   public Pricing scrapPricing(JSONObject productJson) throws MalformedPricingException {
      Double priceFrom = productJson != null ? JSONUtils.getDoubleValueFromJSON(productJson, "real_price", true) : null;
      Double price = productJson != null ? JSONUtils.getDoubleValueFromJSON(productJson, "balance_price", true) : null;

      if (price == null || price.equals(priceFrom)) {
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

   protected boolean isProductPage(JSONObject productJson) {
      return productJson.length() > 0;
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
}
