package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.models.Request;
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
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;


/**
 * Data: 05/02/2021
 *
 * @author BuSSoLoTTi
 */

public abstract class FalabellaCrawler extends Crawler {

   private final String SELLER_FULL_NAME = getSellerName();
   private final String HOME_PAGE = getHomePage();
   private final String API_CODE = getApiCode();
   protected Set<Card> cards = Sets.newHashSet(Card.VISA,Card.MASTERCARD,Card.AMEX);


   public FalabellaCrawler(Session session) {
      super(session);
   }

   protected abstract String getHomePage();

   protected abstract String getApiCode();

   protected abstract String getSellerName();


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "div[data-id]", "data-id");
         String internalPid = internalId;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "div[data-name]", true);
         boolean available = doc.select(".availability span").size() > 1;
         Offers offers = available ? scrapOffers(doc) : null;
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb");

         List<String> images = scrapImages(internalId);
         String primaryImage = images.remove(0);

         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#productInfoContainer"));

         RatingsReviews ratingsReviews = scrapRatingsReviews(doc, internalId);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setOffers(offers)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setDescription(description)
            .setRatingReviews(ratingsReviews)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private RatingsReviews scrapRatingsReviews(Document doc, String internalId) {
      RatingsReviews ratingsReviews = new RatingsReviews();

      ratingsReviews.setDate(session.getDate());
      ratingsReviews.setInternalId(internalId);

      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingsReviews(doc, internalId);

      ratingsReviews.setAdvancedRatingReview(advancedRatingReview);
      ratingsReviews.setAverageOverallRating(CrawlerUtils.extractRatingAverageFromAdvancedRatingReview(advancedRatingReview));
      ratingsReviews.setTotalRating(CrawlerUtils.extractReviwsNumberOfAdvancedRatingReview(advancedRatingReview));

      return ratingsReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingsReviews(Document doc, String internalId) {
      AdvancedRatingReview advancedRatingReview = new AdvancedRatingReview();

      String url = "https://api.bazaarvoice.com/data/display/0.2alpha/product/summary?PassKey=m8bzx1s49996pkz12xvk6gh2e&productid=" + internalId + "&contentType=reviews&reviewDistribution=primaryRating&rev=0";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .build();

      String response = dataFetcher.get(session, request).getBody();

      JSONObject json = response != null ? new JSONObject(response) : new JSONObject();


      advancedRatingReview.setTotalStar1(JSONUtils.getValueRecursive(json, "reviewSummary.primaryRating.distribution.4.count", Integer.class));
      advancedRatingReview.setTotalStar2(JSONUtils.getValueRecursive(json, "reviewSummary.primaryRating.distribution.3.count", Integer.class));
      advancedRatingReview.setTotalStar3(JSONUtils.getValueRecursive(json, "reviewSummary.primaryRating.distribution.2.count", Integer.class));
      advancedRatingReview.setTotalStar4(JSONUtils.getValueRecursive(json, "reviewSummary.primaryRating.distribution.1.count", Integer.class));
      advancedRatingReview.setTotalStar5(JSONUtils.getValueRecursive(json, "reviewSummary.primaryRating.distribution.0.count", Integer.class));

      return advancedRatingReview;
   }

   private List<String> scrapImages(String internalId) {
      List<String> images = new ArrayList<>();

      Request request = Request.RequestBuilder.create()
         .setUrl("https://falabella.scene7.com/is/image/" + API_CODE + "/" + internalId + "?req=set,json")
         .build();
      String response = dataFetcher.get(session, request).getBody();
      String json = CommonMethods.substring(response, "(", ")");
      int index = json.lastIndexOf(",");
      json = json.substring(0, index);

      JSONObject jsonObject = !json.isEmpty() ? new JSONObject(json) : new JSONObject();

      JSONArray imgCodes = JSONUtils.getValueRecursive(jsonObject, "set.item", JSONArray.class);

      for (Object obj : imgCodes) {
         String value = JSONUtils.getValueRecursive(obj, "s.n", String.class);
         String size = JSONUtils.getValueRecursive(obj, "dx", String.class);
         String url = "https://falabella.scene7.com/is/image/" + value + "?wid=" + size;
         images.add(url);
      }

      return images;

   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("div[data-id]")!= null;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      Pricing pricing = scrapPricing(doc);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setPricing(pricing)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {

      Double priceFrom = null;
      Double price = null;

      Element e = doc.selectFirst("li[data-normal-price]");

      if (e!=null) {
         price = CrawlerUtils.scrapDoublePriceFromHtml(doc, "li[data-internet-price]", "data-internet-price", true, '.', session);
         priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "li[data-normal-price]", "data-normal-price", true, '.', session);
      } else {
         price = CrawlerUtils.scrapDoublePriceFromHtml(doc, "li[data-internet-price]", "data-internet-price", true, '.', session);
      }

      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(price)
         .build());

      for (Card card : cards) {

         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setIsShopCard(false)
            .setBrand(card.toString())
            .setInstallments(installments)
            .build());
      }

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(price)
         .setCreditCards(creditCards)
         .build();
   }
}
