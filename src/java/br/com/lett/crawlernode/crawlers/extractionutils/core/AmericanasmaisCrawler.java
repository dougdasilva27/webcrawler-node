package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import com.google.common.net.HttpHeaders;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;

public class AmericanasmaisCrawler extends Crawler {

   public AmericanasmaisCrawler(Session session) {
      super(session);
   }

   private final String storeId = getStoreId();
   private final String sellerName = getSellerName();
   private static final String HOME_PAGE = "https://www.americanas.com.br/lojas-proximas/33014556000196/";

   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString());

   public String getSellerName() {
      return session.getOptions().optString("seller_name");
   }

   public String getStoreId() {
      return session.getOptions().optString("store_id");
   }

   protected Map<String, String> headers = getHeaders();

   private static final List<String> UserAgent = Arrays.asList(
      "Mozilla/5.0 (iPhone; CPU iPhone OS 14_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/93.0.4577.39 Mobile/15E148 Safari/604.1",
      "Mozilla/5.0 (iPad; CPU OS 14_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/93.0.4577.39 Mobile/15E148 Safari/604.1",
      "Mozilla/5.0 (iPod; CPU iPhone OS 14_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/93.0.4577.39 Mobile/15E148 Safari/604.1",
      "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.62 Mobile Safari/537.36",
      "Mozilla/5.0 (Linux; Android 10; SM-A205U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.62 Mobile Safari/537.36",
      "Mozilla/5.0 (Linux; Android 10; SM-A102U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.62 Mobile Safari/537.36",
      "Mozilla/5.0 (Linux; Android 10; SM-G960U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.62 Mobile Safari/537.36",
      "Mozilla/5.0 (Linux; Android 10; LM-X420) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.62 Mobile Safari/537.36",
      "Mozilla/5.0 (Linux; Android 10; LM-Q710(FGN)) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.62 Mobile Safari/537.36",
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"
   );

   public static Map<String, String> getHeaders() {
      Random random = new Random();

      Map<String, String> headers = new HashMap<>();

      headers.put("user-agent", UserAgent.get(random.nextInt(UserAgent.size())));
      headers.put(HttpHeaders.REFERER, HOME_PAGE);
      headers.put("authority", "www.americanas.com.br");
      headers.put(
         HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"
      );
      headers.put(HttpHeaders.CACHE_CONTROL, "max-age=0");
      headers.put(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
      headers.put("sec-fetch-site", "none");
      headers.put("sec-fetch-mode", "navigate");
      headers.put("sec-fetch-user", "?1");
      headers.put("sec-fetch-dest", "document");

      return headers;
   }

   @Override
   protected Object fetch() {

      String internalId = getProductId();
      String url = HOME_PAGE + storeId + "/pick-up?ordenacao=relevance&conteudo=" + internalId;

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .mustSendContentEncoding(false)
         .setHeaders(headers)
         .setFetcheroptions(
            FetcherOptions.FetcherOptionsBuilder.create()
               .mustUseMovingAverage(false)
               .mustRetrieveStatistics(true)
               .build()
         ).setProxyservice(
            Arrays.asList(
               ProxyCollection.NETNUT_RESIDENTIAL_ROTATE_BR
            )
         ).build();

      Response response = CrawlerUtils.retryRequest(request, session, new ApacheDataFetcher(), true);
      String content = response.getBody();

      return Jsoup.parse(content);

   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      JSONObject apolloJson = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.__APOLLO_STATE__ =", null, false, true);
      JSONObject json = extractProductFromApollo(apolloJson);

      if (json.has("products")) {

         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String internalId = getProductId();
         JSONObject productData = (JSONObject) json.optQuery("/products/0/product");

         if (productData != null) {
            String name = productData.optString("name");
            List<String> images = scrapImages(productData);
            String description = scrapDescription(apolloJson);
            String primaryImage = images != null && images.size() > 0 ? images.remove(0) : null;
            JSONObject dataOffers = SaopauloB2WCrawlersUtils.getJson(apolloJson, "OffersResult");
            Offers offers = dataOffers != null ? scrapOffers(dataOffers, doc) : new Offers();
            RatingsReviews rating = scrapRating(productData);

            // Creating the product
            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalId)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(images)
               .setDescription(description)
               .setRatingReviews(rating)
               .setOffers(offers)
               .build();

            products.add(product);
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private List<String> scrapImages(JSONObject productData) {
      List<String> images = new ArrayList<>();
      JSONArray imagesData = productData.optJSONArray("images");
      if (imagesData != null) {
         for (Object o : imagesData) {
            if (o instanceof JSONObject) {
               JSONObject imageData = (JSONObject) o;
               String image = imageData.optString("large");
               if (image != null) {
                  images.add(image);
               }
            }
         }

      }
      return images;
   }

   private static JSONObject extractProductFromApollo(JSONObject apollo) {
      JSONObject product = new JSONObject();

      JSONObject root = apollo.optJSONObject("ROOT_QUERY");
      if (root != null) {
         for (String key : root.keySet()) {
            if (key.startsWith("search")) {
               product = root.optJSONObject(key);
               break;
            }
         }
      }

      return product;
   }

   private String scrapDescription(JSONObject apollo) {
      StringBuilder description = new StringBuilder();

      JSONObject productInfoJson = JSONUtils.getValueRecursive(apollo, "ROOT_QUERY.product:{\"productId\":\"" + getProductId() + "\"}", ".", JSONObject.class, new JSONObject());

      description.append(JSONUtils.getValueRecursive(productInfoJson, "description.content", ".", String.class, ""));
      description.append("\n");

      JSONArray attributes = productInfoJson.optJSONArray("attributes");

      for (Object attr : attributes) {
         JSONObject attributeJson = (JSONObject) attr;
         if ("Attribute".equals(attributeJson.optString("__typename"))) {
            description.append(attributeJson.optString("name"));
            description.append(": ");
            description.append(attributeJson.optString("value"));
            description.append("\n");
         }
      }

      return description.toString();
   }

   private String getProductId() {
      String[] extractId = session.getOriginalURL().split("&conteudo=");
      if (extractId.length > 0) {
         return extractId[1];
      }

      return null;
   }

   private Offers scrapOffers(JSONObject dataOffers, Document doc) throws OfferException, MalformedPricingException {

      Offers offers = new Offers();
      Pricing pricing = scrapPricing(dataOffers);
      List<String> sales = scrapSales(doc);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(sellerName)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private List<String> scrapSales(Document doc) {
      List<String> sales = new ArrayList<>();
      String sale = CrawlerUtils.scrapStringSimpleInfo(doc, "p[class^=\"styles__BadgeText\"]", true);
      if (sale != null) {
         sales.add(sale);
      }
      return sales;
   }


   private Pricing scrapPricing(JSONObject dataOffers) throws MalformedPricingException {
      Double spotlightPrice = dataOffers.optDouble("salesPrice");
      Double priceFrom = dataOffers.optDouble("listPrice", 0.0);
      if (priceFrom == 0.0 || priceFrom.equals(spotlightPrice)) {
         priceFrom = null;
      }
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
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

   private RatingsReviews scrapRating(JSONObject productData) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      JSONObject dataRating = productData.optJSONObject("rating");
      if (dataRating != null) {
         Integer totalNumOfEvaluations = JSONUtils.getIntegerValueFromJSON(dataRating, "reviews", 0);
         Double avgRating = JSONUtils.getDoubleValueFromJSON(dataRating, "average", true);

         ratingReviews.setTotalRating(totalNumOfEvaluations);
         ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
         ratingReviews.setAverageOverallRating(avgRating);
      }
      return ratingReviews;
   }

}
