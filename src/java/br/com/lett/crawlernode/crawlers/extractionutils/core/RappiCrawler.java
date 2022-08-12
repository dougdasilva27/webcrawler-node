package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
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
import java.util.regex.Pattern;


public abstract class RappiCrawler extends Crawler {

   public RappiCrawler(Session session) {
      super(session);
      this.config.setFetcher(FetchMode.APACHE);
   }

   protected String getStoreId() {
      return session.getOptions().optString("storeId");
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

   private boolean checkOldUrl(String productUrl) throws MalformedURLException {
      URL url = new URL(productUrl);
      final Pattern urlPathPattern = Pattern.compile(".*/([0-9][^a-zA-Z]*)_([0-9][^a-zA-Z]*)");

      boolean checkHost = url.getHost().contains("www.rappi.com");
      boolean checkPath = urlPathPattern.matcher(url.getPath()).matches();

      return checkHost && checkPath;
   }

   protected String getProductIdFromRanking(JSONObject productJson) {
      String productName = productJson.optString("name");
      String productImage = productJson.optString("image");
      String productNameEncoded = productName != null ? StringUtils.stripAccents(productName).replace(" ", "%20") : null;
      String url = getMarketBaseUrl() + getStoreId() + "/s?term=" + productNameEncoded;
      Request request = Request.RequestBuilder.create()
         .setCookies(this.cookies)
         .setUrl(url)
         .setFollowRedirects(true)
         .build();

      Response response = dataFetcher.get(session, request);

      Document docRanking = Jsoup.parse(response.getBody());
      JSONObject rankingPageJson = CrawlerUtils.selectJsonFromHtml(docRanking, "#__NEXT_DATA__", null, null, false, false);
      JSONArray searchProducts = JSONUtils.getValueRecursive(rankingPageJson, "props.pageProps.products", JSONArray.class, new JSONArray());

      String productFoundInternalId = null;
      if (searchProducts.length() > 0) {
         for (int i = 0; i < searchProducts.length(); i++) {
            JSONObject searchProduct = searchProducts.getJSONObject(i);
            String imageProductSearch = searchProduct.optString("image");
            if (imageProductSearch.equals(productImage)) {
               productFoundInternalId = searchProduct.optString("product_id");
               break;
            }
         }
      }

      return productFoundInternalId;
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

   protected JSONObject fetchProductApi(String productId, String token) {
      String url = "https://services." + getHomeDomain() + "/api/ms/web-proxy/dynamic-list/cpgs";

      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "application/json, text/plain, */*");
      headers.put("language", "pt");
      headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36");
      headers.put("content-type", "application/json");
      headers.put("authorization", token);

      String productFriendlyUrl = getStoreId() + "_" + productId;

      String payload = "{\"dynamic_list_request\":{\"context\":\"product_detail\",\"state\":{\"lat\":\"1\",\"lng\":\"1\"},\"limit\":100,\"offset\":0},\"dynamic_list_endpoint\":\"context/content\",\"proxy_input\":{\"product_friendly_url\":\"" + productFriendlyUrl + "\"}}";


      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .build();

      String body = this.dataFetcher.post(session, request).getBody();

      JSONObject jsonObject = CrawlerUtils.stringToJSONObject(body);

      return JSONUtils.getValueRecursive(jsonObject, "dynamic_list_response.data.components.0.resource.product", ".", JSONObject.class, new JSONObject());
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {

      List<Product> products = new ArrayList<>();

      String storeId = getStoreId();

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
         String url = getHomeCountry() + getUrlPrefix() + getStoreId() + "_" + internalId;

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
