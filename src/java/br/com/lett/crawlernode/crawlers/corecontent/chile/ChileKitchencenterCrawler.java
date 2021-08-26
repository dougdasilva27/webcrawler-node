package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Date: 10/12/2018
 *
 * @author Gabriel Dornelas
 */
public class ChileKitchencenterCrawler extends Crawler {

   private static final String HOME_PAGE = "http://www.kitchencenter.cl/";
   private static final String SELLER_FULL_NAME = "kitchencenter";
   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString());

   public ChileKitchencenterCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".col select option", "data-product-id");
         Elements variations = doc.select(".col select option");
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".specification", ".short-description"));
         RatingsReviews ratingsReviews = getRatingsReviews(internalPid);

         for (Element e : variations) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".col select option", "value");
            String name = getName(doc, e);
            boolean available = true; // I not found any product unavailable
            Offers offers = available ? scrapOffers(e, doc) : new Offers();
            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".col-lg-7 .d-none div [data-variant-title=\"" + CommonMethods.getLast(name.split("-")) + "\"]", Arrays.asList("data-flickity-lazyload-src"), "https:",
               "");
            List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".col-lg-7 .d-none div [data-variant-title=\"" + CommonMethods.getLast(name.split("-")) + "\"]", Arrays.asList("data-flickity-lazyload-src"), "https:",
               "", primaryImage);

            // Creating the product
            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setRatingReviews(ratingsReviews)
               .setOffers(offers)
               .build();

            products.add(product);
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".product-details-wrapper").isEmpty();
   }

   private String getName(Document doc, Element e) {
      StringBuilder buildName = new StringBuilder();
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".h2", false);
      String color = CrawlerUtils.scrapStringSimpleInfo(e, null, false);

      buildName.append(name).append(" - ");

      if (color != null) {
         buildName.append(color.split("-")[0]);

      }

      return buildName.toString();
   }

   private Offers scrapOffers(Element element, Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(element, doc);
      List<String> sales = scrapSales(pricing);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setSales(sales)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;

   }


   private Pricing scrapPricing(Element element, Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(element, null, null, true, '.', session);
      String price = getPriceFrom(doc);
      Double priceFrom = price != null ? Double.parseDouble(price) : null;
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
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

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();
      String saleDiscount = CrawlerUtils.calculateSales(pricing);

      if (saleDiscount != null) {
         sales.add(saleDiscount);
      }

      return sales;
   }

   private String getPriceFrom(Document doc) {
      String priceInformation = getPriceInformation(doc);
      String price = null;
      if (priceInformation != null) {
         Pattern pattern = Pattern.compile("CompareAtPrice: \"\\$(.*)\"");
         Matcher matcher = pattern.matcher(priceInformation);
         if (matcher.find()) {
            price = matcher.group(1);
         }
      }
      return price;
   }

   private String getPriceInformation(Document doc) {
      Elements scripts = doc.select("script[type='text/javascript']");
      for (Element e : scripts) {
         String script = e.html();

         if (script.contains("var _learnq = _")) {
            return script;
         }
      }
      return null;
   }


   private JSONObject getReviewsJson(String internalPid) {
      String url = "https://reviews.hulkapps.com/api/shop/10443915319/reviews?product_id=" + internalPid;

      Request request = Request.RequestBuilder.create().setUrl(url).build();
      String content = this.dataFetcher.get(session, request).getBody();

      return CrawlerUtils.stringToJson(content);
   }


   private RatingsReviews getRatingsReviews(String internalPid) {

      RatingsReviews ratingReviews = new RatingsReviews();
      JSONObject dataReviews = getReviewsJson(internalPid);

      if (dataReviews != null) {

         ratingReviews.setDate(session.getDate());

         Integer totalNumOfEvaluations = dataReviews.optInt("total");
         Double avgRating = dataReviews.optDouble("avg_rating");

         ratingReviews.setTotalRating(totalNumOfEvaluations);
         ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
         ratingReviews.setAverageOverallRating(avgRating);
         ratingReviews.setInternalId(internalPid);

      }

      return ratingReviews;

   }


}
