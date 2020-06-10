package br.com.lett.crawlernode.crawlers.corecontent.portugal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.test.Test;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

public class PortugalElcorteinglesCrawler extends Crawler {

   private static final String HOME_PAGE = "www.elcorteingles.pt/";
   private static final String SELLER_FULL_NAME = "Elcorteingles";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public PortugalElcorteinglesCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   protected Object fetch() {
      Request request = RequestBuilder.create()
            .setUrl(session.getOriginalURL())
            .setCookies(this.cookies)
            .mustSendContentEncoding(false)
            .setFetcheroptions(
                  FetcherOptionsBuilder.create()
                        .mustUseMovingAverage(false)
                        .mustRetrieveStatistics(true)
                        .build()
            ).setProxyservice(
                  Arrays.asList(
                        ProxyCollection.INFATICA_RESIDENTIAL_BR,
                        ProxyCollection.STORM_RESIDENTIAL_EU,
                        ProxyCollection.STORM_RESIDENTIAL_US,
                        ProxyCollection.BUY
                  )
            ).build();

      String content = this.dataFetcher.get(session, request).getBody();

      if (content == null || content.isEmpty()) {
         content = new ApacheDataFetcher().get(session, request).getBody();
      }

      return Jsoup.parse(content);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      CommonMethods.saveDataToAFile(doc, Test.pathWrite + "PORTUGA.html");

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#pid", "data-product-id");
         String internalPid = internalId;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "#product-info .title", false);
         boolean available = doc.selectFirst("#pid .add-to-cart.add") != null;
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#product-images img", Arrays.asList("src"), "http:", HOME_PAGE);
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#alternate-images li img", Arrays.asList("src"), "http:", HOME_PAGE, primaryImage);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "#breadcrumbs li a");
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#features"));
         Offers offers = available ? scrapOffer(doc) : new Offers();

         JSONObject ratingApi = apiRatingReview(doc, internalId);

         RatingsReviews ratingReview = scrapRatingReviews(ratingApi);
         // Creating the product

         Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setOffers(offers)
               .setRatingReviews(ratingReview)
               .build();
         products.add(product);


      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }


   private boolean isProductPage(Document document) {
      return document.selectFirst(".vp.content-wrapper") != null;
   }

   private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(doc);

      offers.add(OfferBuilder.create()
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

   private List<String> scrapSales(Document doc) {
      List<String> sales = new ArrayList<>();

      Element salesOneElement = doc.selectFirst(".discount");
      String firstSales = salesOneElement != null ? salesOneElement.text() : null;

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }

      return sales;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".former.stroked", null, false, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".current.sale", null, false, ',', session);
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

      return PricingBuilder.create()
            .setPriceFrom(priceFrom)
            .setSpotlightPrice(spotlightPrice)
            .setCreditCards(creditCards)
            .build();

   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
         installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(1)
               .setInstallmentPrice(spotlightPrice)
               .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCardBuilder.create()
               .setBrand(card)
               .setInstallments(installments)
               .setIsShopCard(false)
               .build());
      }

      return creditCards;
   }

   private String getPassKey() {
      String passKey = null;

      String urlApi = "https://display.ugc.bazaarvoice.com/static/elcorteingles-pt/main_site/pt_PT/bvapi.js";
      Request request = RequestBuilder.create().setUrl(urlApi).build();
      String content = this.dataFetcher.get(session, request).getBody();

      if (content != null) {

         Integer indexOf = content.indexOf("passkey:\"");

         if (indexOf != null) {
            Integer lastIndexOf = content.lastIndexOf("\",baseUrl");

            if (lastIndexOf != null) {
               String contentSubstring = content.substring(indexOf, lastIndexOf);

               passKey = contentSubstring.contains("passkey:\"") ? contentSubstring.replace("passkey:\"", "") : null;

            }
         }
      }


      return passKey;
   }

   private JSONObject apiRatingReview(Document doc, String internalId) {
      JSONObject ratingInfo = new JSONObject();

      String passkey = getPassKey();

      String urlApi = "https://api.bazaarvoice.com/data/batch.json?passkey=" + passkey + "&apiversion=5.5&resource.q0=products&filter.q0=id%3Aeq%3A" + internalId + "&stats.q0=reviews";
      Request request = RequestBuilder.create().setUrl(urlApi).build();

      JSONObject jsonObject = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
      JSONObject produtoInfo = JSONUtils.getJSONValue(jsonObject, "BatchedResults");
      JSONObject json = JSONUtils.getJSONValue(produtoInfo, "q0");
      JSONArray result = JSONUtils.getJSONArrayValue(json, "Results");

      if (result != null) {
         for (Object arr : result) {
            JSONObject ReviewStatistics = (JSONObject) arr;

            ratingInfo = JSONUtils.getJSONValue(ReviewStatistics, "ReviewStatistics");
         }
      }
      return ratingInfo;
   }

   private RatingsReviews scrapRatingReviews(JSONObject ratingInfo) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = ratingInfo.optInt("TotalReviewCount");
      Double avgRating = ratingInfo.optDouble("AverageOverallRating");
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(ratingInfo);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(JSONObject ratingInfo) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      JSONArray RatingDistribution = JSONUtils.getJSONArrayValue(ratingInfo, "RatingDistribution");

      for (Object review : RatingDistribution) {

         JSONObject ratingReviews = (JSONObject) review;

         if (ratingReviews != null) {

            Integer RatingValue = ratingReviews.optInt("RatingValue");
            Integer count = ratingReviews.optInt("Count");


            switch (RatingValue) {
               case 5:
                  star5 = count;
                  break;
               case 4:
                  star4 = count;
                  break;
               case 3:
                  star3 = count;
                  break;
               case 2:
                  star2 = count;
                  break;
               case 1:
                  star1 = count;
                  break;
               default:
                  break;
            }
         }

      }

      return new AdvancedRatingReview.Builder()
            .totalStar1(star1)
            .totalStar2(star2)
            .totalStar3(star3)
            .totalStar4(star4)
            .totalStar5(star5)
            .build();
   }



}
