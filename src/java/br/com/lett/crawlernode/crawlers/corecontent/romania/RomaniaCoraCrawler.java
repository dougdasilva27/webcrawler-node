package br.com.lett.crawlernode.crawlers.corecontent.romania;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.http.HttpHeaders;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard;
import models.pricing.CreditCards;
import models.pricing.Installment;
import models.pricing.Installments;
import models.pricing.Pricing;

public class RomaniaCoraCrawler extends Crawler {

   private static final String HOST_PAGE = "www.cora.ro";
   private static final String MAIN_SELLER_NAME = "cora";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.HIPER.toString(), Card.AMEX.toString());

   public RomaniaCoraCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
   }

   @Override
   protected Document fetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
      headers.put(HttpHeaders.REFERER, session.getOriginalURL());

      String payload = "_pecid=fb99d6e4-c23e-4387-80a6-78b159936009&storeId=10251";

      Request req = RequestBuilder.create()
            .setUrl(session.getOriginalURL())
            .setPayload(payload)
            .setHeaders(headers)
            .setCookies(cookies)
            .build();

      return Jsoup.parse(this.dataFetcher.post(session, req).getBody());
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "var product =", "};", false, false);
         String internalId = json.optString("id");
         String internalPid = json.optString("partnumber");

         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".title", true);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb > li > a", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".img-zoom", Collections.singletonList("src"), "https", HOST_PAGE);
         String secondaryImage = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".img-zoom", Collections.singletonList("src"), "https", HOST_PAGE, primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".product-details .tab-content", ".product-details > div > p"));
         Offers offers = scrapAvailability(doc) ? scrapOffers(doc) : new Offers();
         RatingsReviews ratings = scrapRatingReviews(doc);

         Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImage)
               .setDescription(description)
               .setOffers(offers)
               .setRatingReviews(ratings)
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("#product-details") != null;
   }

   private boolean scrapAvailability(Document doc) {
      return doc.selectFirst("#product-details .add-to-cart") != null;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);

      if (pricing != null) {
         offers.add(Offer.OfferBuilder.create()
               .setUseSlugNameAsInternalSellerId(true)
               .setSellerFullName(MAIN_SELLER_NAME)
               .setSellersPagePosition(1)
               .setIsBuybox(false)
               .setIsMainRetailer(true)
               .setPricing(pricing)
               .build());
      }
      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#product-details .price", null, true, '.', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#product-details .cross-price", null, true, '.', session);

      return Pricing.PricingBuilder.create()
            .setPriceFrom(priceFrom)
            .setSpotlightPrice(spotlightPrice)
            .setCreditCards(scrapCreditCards(doc, spotlightPrice))
            .setBankSlip(BankSlipBuilder.create()
                  .setFinalPrice(spotlightPrice)
                  .build())
            .build();
   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice).build());

      for (String card : cards) {
         creditCards.add(
               CreditCard.CreditCardBuilder.create()
                     .setBrand(card)
                     .setInstallments(installments)
                     .setIsShopCard(false)
                     .build());
      }
      return creditCards;
   }

   // When this crawler was made no Advanced Rating Review was found
   private RatingsReviews scrapRatingReviews(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalComments = CrawlerUtils.scrapIntegerFromHtml(doc, ".avg-rating .users", true, 0);
      Double avgRating = scrapAvgRating(doc);

      ratingReviews.setTotalRating(totalComments);
      ratingReviews.setTotalWrittenReviews(totalComments);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setAdvancedRatingReview(new AdvancedRatingReview());

      return ratingReviews;
   }

   private Double scrapAvgRating(Document doc) {
      Double avg = 0d;

      Double percentage = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".avg-rating .average", null, false, '.', session);
      if (percentage != null) {
         avg = percentage;
      }

      return avg;
   }
}
