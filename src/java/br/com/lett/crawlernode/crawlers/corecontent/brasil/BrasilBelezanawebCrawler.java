package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

/**
 * Date: 27/09/2018
 *
 * @author Gabriel Dornelas
 */
public class BrasilBelezanawebCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.belezanaweb.com.br/";
   private static final String SELLER_FULL_NAME = "Beleza na Web Brasil";
   private int stars5 = 0;
   private int stars4 = 0;
   private int stars3 = 0;
   private int stars2 = 0;
   private int stars1 = 0;


   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString(),
      Card.DINERS.toString());


   public BrasilBelezanawebCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
      super.config.setFetcher(FetchMode.FETCHER);
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

         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-sku", true);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".nproduct-title", false);
         boolean available = !doc.select(".product-buy > a").isEmpty();
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb .breadcrumb-item:not(:first-child) > a", false);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-image-wrapper > img", Arrays.asList("data-zoom-image", "src"),
            "https:", "res.cloudinary.com");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".gallery-lightbox .product-image-wrapper > img", Arrays.asList("data-zoom-image", "src"), "https:", "res.cloudinary.com", primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".product-description", ".product-characteristics"));
         RatingsReviews rating = scrapRating(doc);

         Offers offers = available ? scrapOffers(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setName(name)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setRatingReviews(rating)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".product-sku").isEmpty();
   }


   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing);

      offers.add(Offer.OfferBuilder.create()
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

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      if (scrapSaleDiscount(pricing) != null) {
         sales.add(scrapSaleDiscount(pricing));
      }

      return sales;
   }

   private String scrapSaleDiscount(Pricing pricing) {

      return CrawlerUtils.calculateSales(pricing);
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".nproduct-price-value", null, true, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".nproduct-price-max", null, false, ',', session);
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

   private RatingsReviews scrapRating(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtml(doc, ".rating-container .rating-count", false, 0);
      Double avgRating = getTotalAvgRating(doc);
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(doc, totalNumOfEvaluations);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private Double getTotalAvgRating(Document doc) {
      double avgRating = 0d;

      Element avg = doc.selectFirst(".rating-value-container");

      if (avg != null) {
         String text = avg.ownText().replaceAll("[^0-9.]", "").trim();

         if (!text.isEmpty()) {
            avgRating = Double.parseDouble(text);
         }
      }

      return avgRating;
   }


   private Document htmlToCaptureStars(Document doc, int page) {

      Map<String, String> headers = new HashMap<>();
      headers.put("referer", session.getOriginalURL());

      String captureIds = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "a.js-load-more", "data-ajax");
      String apiIds = captureIds != null ? captureIds.split("\\?")[0] : null;
      String url = "https://www.belezanaweb.com.br" + apiIds + "?pagina=" + page + "&size=50";
      Request request = Request.RequestBuilder.create().setUrl(url).setHeaders(headers).build();
      String content = this.dataFetcher.get(session, request).getBody();

      return Jsoup.parse(content);

   }

   private int hasNextPage(Integer totalNumOfEvaluations) {

      if (totalNumOfEvaluations % 50 > 0) {

         return totalNumOfEvaluations / 50 + 1;

      } else {

         return totalNumOfEvaluations / 50;

      }

   }

   /* When the product have variations, ex: size -> 250ml, 500ml, or color, the quantity of reviews on site, show a total for all ids.
    https://www.belezanaweb.com.br/loreal-professionnel-serie-expert-blondifier-gloss-mascara-capilar-500ml/
    https://www.belezanaweb.com.br/maybelline-instant-age-rewind-eraser-dark-circles-120-light-corretivo-liquido-59ml/
    */

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc, Integer totalNumOfEvaluations) {
      AdvancedRatingReview advancedRatingReview = new AdvancedRatingReview();

      if (totalNumOfEvaluations <= 10) {
         advancedRatingReview = scrapAdvancedRating(doc);

      } else if (totalNumOfEvaluations <= 50) {
         Document apiDoc = htmlToCaptureStars(doc, 1);
         if (apiDoc != null) {
            advancedRatingReview = scrapAdvancedRating(apiDoc);

         }
      } else {

         for (int i = 1; i <= hasNextPage(totalNumOfEvaluations); i++) {
            Document apiDoc = htmlToCaptureStars(doc, i);
            if (apiDoc != null) {
               advancedRatingReview = scrapAdvancedRating(apiDoc);

            }
         }
      }

      return advancedRatingReview;
   }


   private AdvancedRatingReview scrapAdvancedRating(Document doc) {

      Elements elements = doc.select(".review .container-star img");

      if (!elements.isEmpty()) {
         for (Element element : elements) {

            int starNumber = CrawlerUtils.scrapIntegerFromHtmlAttr(element, null, "alt", 0);

            switch (starNumber) {
               case 50:
                  this.stars5 += 1;
                  break;
               case 40:
                  this.stars4 += 1;
                  break;
               case 30:
                  this.stars3 += 1;
                  break;
               case 20:
                  this.stars2 += 1;
                  break;
               case 10:
                  this.stars1 += 1;
                  break;
               default:
                  break;
            }


         }


      }

      return new AdvancedRatingReview.Builder()
         .totalStar1(this.stars1)
         .totalStar2(this.stars2)
         .totalStar3(this.stars3)
         .totalStar4(this.stars4)
         .totalStar5(this.stars5)
         .build();

   }


}
