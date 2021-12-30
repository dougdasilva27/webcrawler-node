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
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

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
      JSONObject apolloJson = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.__APOLLO_STATE__ =", null, false, true);
      JSONObject json = extractProductFromApollo(apolloJson);

      if (json.has("products")) {

         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String internalId = getProductId();
         JSONObject productData = (JSONObject) json.optQuery("/products/0/product");

         if (productData != null) {
            String name = productData.optString("name");
            String primaryImage = (String) productData.optQuery("/images/0/large");
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
