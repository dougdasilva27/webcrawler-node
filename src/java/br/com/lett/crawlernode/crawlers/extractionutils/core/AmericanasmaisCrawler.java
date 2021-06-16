package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
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
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

public abstract class AmericanasmaisCrawler extends Crawler {

   protected AmericanasmaisCrawler(Session session) {
      super(session);
   }

   private final String storeId = getStoreId();

   private final String sellerName = getSellerName();
   private static final String HOME_PAGE = "https://www.americanas.com.br/lojas-proximas/33014556000196/";

   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString());

   public String getStoreId() {
      return storeId;
   }

   public String getSellerName() {
      return sellerName;
   }

   @Override
   protected Object fetch() {

      String internalId = getProductId();
      String url = HOME_PAGE + storeId + "/pick-up?ordenacao=relevance&conteudo=" + internalId;

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .mustSendContentEncoding(false)
         .setFetcheroptions(
            FetcherOptions.FetcherOptionsBuilder.create()
               .mustUseMovingAverage(false)
               .mustRetrieveStatistics(true)
               .build()
         ).setProxyservice(
            Arrays.asList(
               ProxyCollection.INFATICA_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY
            )
         ).build();


      Response response = new JsoupDataFetcher().get(session, request);
      String content = response.getBody();

      int statusCode = response.getLastStatusCode();

      if ((Integer.toString(statusCode).charAt(0) != '2' &&
         Integer.toString(statusCode).charAt(0) != '3'
         && statusCode != 404)) {
         request.setProxyServices(Arrays.asList(
            ProxyCollection.INFATICA_RESIDENTIAL_BR,
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR));

         content = new FetcherDataFetcher().get(session, request).getBody();
      }

      return Jsoup.parse(content);

   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      JSONObject json = selectJsonFromHtml(doc);

      if (json.has("products")) {

         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String internalId = getProductId();
         JSONObject productData = (JSONObject) json.optQuery("/products/" + internalId);

         if (productData != null) {
            String name = productData.optString("name");
            Collection<String> categories = categories(json); //Has only one category
            JSONArray imageJson = productData.optJSONArray("images");
            List<String> images = imageJson != null ? CrawlerUtils.scrapImagesListFromJSONArray(imageJson, "extraLarge", null, "https", "images-americanas.b2w.io/", session) : null;
            String primaryImage = images != null && !images.isEmpty() ? images.remove(0) : null;
            String description = getDescription(productData);
            JSONObject dataOffers = (JSONObject) productData.query("/offers/result/0");
            Offers offers = dataOffers != null ? scrapOffers(dataOffers, doc) : new Offers();
            RatingsReviews rating = scrapRating(productData);


            // Creating the product
            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalId)
               .setName(name)
               .setCategories(categories)
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

   public JSONObject selectJsonFromHtml(Document doc) throws JSONException, ArrayIndexOutOfBoundsException, IllegalArgumentException, UnsupportedEncodingException {
      JSONObject jsonObject = new JSONObject();
      Elements scripts = doc.select("body > script");

      for (Element e : scripts) {
         String script = e.html();
         if (script.contains("window.__PRELOADED_STATE__ =")) {
            String split = CrawlerUtils.extractSpecificStringFromScript(script, "window.__PRELOADED_STATE__ = \"", true, "}", true)
               .replace("undefined", "\"undefined\"")
               .replace("\"\"undefined\"\"", "undefined") + "}";
            jsonObject = CrawlerUtils.stringToJson(split);
            break;
         }
      }
      return jsonObject;
   }


   private String getProductId() {
      String[] extractId = session.getOriginalURL().split("&conteudo=");
      if (extractId.length > 0) {
         return extractId[1];
      }

      return null;
   }

   private String getDescription(JSONObject productData) {
      StringBuilder stringBuilder = new StringBuilder();
      String description = productData.optString("description");
      if (description != null) {
         stringBuilder.append("descrição: ");
         stringBuilder.append(description);
      }
      JSONArray properties = JSONUtils.getValueRecursive(productData, "attributes.0.properties", JSONArray.class);
      if (properties != null) {
         stringBuilder.append("informações técnicas: ");
         for (Object obj : properties) {
            if (obj instanceof JSONObject) {
               JSONObject property = (JSONObject) obj;
               String name = property.optString("name");
               String value = property.optString("value");

               if (name != null && value != null) {
                  stringBuilder.append(name).append(": ").append(value);
               }

            }
         }
      }

      return stringBuilder.toString();
   }

   private Collection<String> categories(JSONObject json) {
      Collection<String> categories = new ArrayList<>();
      String category = JSONUtils.getValueRecursive(json, "aggregations.categories.0.options.0.value", String.class);
      if (category != null) {
         categories.add(category);
      }
      return categories;
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
      String sale = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-grid-item .TextUI-xlll2j-3", true);
      if (sale != null) {
         sales.add(sale);
      }
      return sales;
   }


   private Pricing scrapPricing(JSONObject dataOffers) throws MalformedPricingException {
      Double spotlightPrice = dataOffers.optDouble("salesPrice");
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();
      //Site hasn't any product with old price

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
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
