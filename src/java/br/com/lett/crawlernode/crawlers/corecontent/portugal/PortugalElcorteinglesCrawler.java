package br.com.lett.crawlernode.crawlers.corecontent.portugal;

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
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;

public class PortugalElcorteinglesCrawler extends Crawler {

   private static final String HOME_PAGE = "www.elcorteingles.pt/supermercado";
   private static final String SELLER_FULL_NAME = "El Corte Ingles";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public PortugalElcorteinglesCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
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
         .setTimeout(20000)
         .setFetcheroptions(
            FetcherOptionsBuilder.create()
               .mustUseMovingAverage(false)
               .mustRetrieveStatistics(true)
               .build())
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.INFATICA_RESIDENTIAL_BR,
               ProxyCollection.NETNUT_RESIDENTIAL_ES,
               ProxyCollection.BUY
            )
         ).build();


      String content = this.dataFetcher.get(session, request).getBody();


      return Jsoup.parse(content);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".dataholder", "data-product-id");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc,".pdp-title", false);
         boolean available = doc.selectFirst(".product_controls-button._buy") != null;
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".elements_slider-slide img", Collections.singletonList("src"), "https", "cdn.grupoelcorteingles.es");
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs-item a");
         String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList(".pdp-info-container"));
         Offers offers = available ? scrapOffer(doc) : new Offers();

         JSONObject ratingApi = apiRatingReview(internalId);

         RatingsReviews ratingReview = scrapRatingReviews(ratingApi);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
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
      return document.selectFirst(".full_vp.pdp-content-vp") != null;
   }

   private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);

      offers.add(OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Element elemPrice;
      Element elemPriceFrom = null;
      if (doc.selectFirst(".prices-price._offer") != null) {
         elemPrice = doc.selectFirst(".prices-price._offer");
         elemPriceFrom = doc.selectFirst(".prices-price._before");
      } else {
         elemPrice = doc.selectFirst(".prices-price");
      }
      Double spotlightPrice = MathUtils.parseDoubleWithComma(elemPrice.text());
      Double priceFrom = null;
      if (elemPriceFrom != null) {
         priceFrom = MathUtils.parseDoubleWithComma(elemPriceFrom.text());
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();

   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      if (spotlightPrice != null) {

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
      }

      return creditCards;
   }


   private JSONObject apiRatingReview(String internalId) {
      JSONObject ratingInfo = new JSONObject();

      String urlApi = "https://api.bazaarvoice.com/data/batch.json?passkey=canv5L9pzGrdW3dOOxZrXJ9ZdveMke1FtkDsBrPV8SvAg&apiversion=5.5&resource.q0=products&filter.q0=id%3Aeq%3A" + internalId + "&stats.q0=reviews";

      Request request = RequestBuilder.create().setUrl(urlApi).build();

      JSONObject jsonObject = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
      JSONObject produtoInfo = JSONUtils.getJSONValue(jsonObject, "BatchedResults");
      JSONObject json = JSONUtils.getJSONValue(produtoInfo, "q0");
      JSONArray result = JSONUtils.getJSONArrayValue(json, "Results");

      if (result != null) {
         for (Object arr : result) {
            JSONObject ReviewStatistics = (JSONObject) arr;

            if (!ReviewStatistics.isEmpty()) {
               ratingInfo = JSONUtils.getJSONValue(ReviewStatistics, "ReviewStatistics");
            }
         }
      }
      return ratingInfo;
   }

   private RatingsReviews scrapRatingReviews(JSONObject ratingInfo) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = ratingInfo.optInt("TotalReviewCount");
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(ratingInfo);
      Double avgRating = ratingInfo.optDouble("AverageOverallRating", 0D);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(JSONObject ratingInfo) {
      Map<Integer, Integer> rating = null;

      JSONArray ratingDistribution = JSONUtils.getJSONArrayValue(ratingInfo, "RatingDistribution");
      if (ratingDistribution.length() > 0) rating = new HashMap<>();

      for (Object review : ratingDistribution) {

         JSONObject ratingReviews = (JSONObject) review;

         if (ratingReviews != null && rating != null) {
            rating.put(ratingReviews.optInt("RatingValue"), ratingReviews.optInt("Count"));
         }
      }
      AdvancedRatingReview.Builder advRatingReview = new AdvancedRatingReview.Builder();

      if (rating != null) advRatingReview.allStars(rating);

      return advRatingReview.build();
   }
}
