package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
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
import models.Offers;
import models.pricing.*;
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

public abstract class RappiRestauranteCrawler extends Crawler {

   public RappiRestauranteCrawler(Session session) {
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

   private String extractInternalIdFromUrl(String url) {
      final String regex = "productDetail=(\\d+)";
      final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      final Matcher matcher = pattern.matcher(url);

      return matcher.find() && matcher.groupCount() > 0 ? matcher.group(1) : null;
   }

   protected JSONObject fetchProduct(Document doc, String internalId) {
      JSONObject productsInfo = new JSONObject();

      JSONObject pageJson = CrawlerUtils.selectJsonFromHtml(doc, "#__NEXT_DATA__", null, null, false, false);

      JSONArray corridors = new JSONArray();
      JSONObject fallback = JSONUtils.getValueRecursive(pageJson, "props.pageProps.fallback", JSONObject.class, new JSONObject());
      if (!fallback.isEmpty()) {
         Iterator<String> keys = fallback.keys();
         while(keys.hasNext()) {
            String key = keys.next();
            if (fallback.get(key) instanceof JSONObject) {
               corridors = JSONUtils.getValueRecursive(fallback.get(key), "corridors", JSONArray.class, new JSONArray());
               break;
            }
         }
      }

      for (Object corridor : corridors) {
         JSONObject jsonCorridor = (JSONObject) corridor;
         JSONArray products = jsonCorridor.optJSONArray("products");
         for (Object product : products) {
            JSONObject jsonProduct = (JSONObject) product;
            if (jsonProduct.optString("productId", "").equals(internalId)) {
               productsInfo = jsonProduct;
               return productsInfo;
            }
         }
      }

      return productsInfo;
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {

      List<Product> products = new ArrayList<>();

      String storeId = getStoreId();

      JSONObject productJson = fetchProduct(doc, extractInternalIdFromUrl(this.session.getOriginalURL()));


      if (isProductPage(productJson)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = storeId + "_" + productJson.optString("productId");
         String internalId = productJson.optString("productId");
         String description = productJson.optString("description");
         String primaryImage = CrawlerUtils.completeUrl(productJson.optString("image"), "https", getImagePrefix());
         String name = productJson.optString("name");
         boolean available = productJson.optBoolean("isAvailable");
         Offers offers = available ? scrapOffers(productJson) : new Offers();
         String url = this.session.getOriginalURL();

         Product product = ProductBuilder.create()
            .setUrl(url)
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
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

   public Offers scrapOffers(JSONObject productJson) {
      Offers offers = new Offers();

      try {
         Pricing pricing = scrapPricing(productJson);
         List<String> sales = scrapSales(pricing);

         Offer offer = new Offer.OfferBuilder()
            .setSellerFullName("Rappi Restaurantes")
            .setInternalSellerId(null)
            .setUseSlugNameAsInternalSellerId(true)
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
      Double priceFrom = productJson != null ? JSONUtils.getDoubleValueFromJSON(productJson, "realPrice", true) : null;
      Double price = productJson != null ? JSONUtils.getDoubleValueFromJSON(productJson, "priceNumber", true) : null;

      if (price == null || price.equals(priceFrom)) {
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

   protected boolean isProductPage(JSONObject productJson) {
      return productJson.length() > 0;
   }
}
