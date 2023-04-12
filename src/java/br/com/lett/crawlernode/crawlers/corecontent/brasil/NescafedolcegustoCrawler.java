package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.*;
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
import org.jsoup.select.Elements;

import java.util.*;

public class NescafedolcegustoCrawler extends Crawler {

   private static String SELLER_NAME = "nescafe dolcegusto";

   public Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.ELO.toString());

   public NescafedolcegustoCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = getInternalId(doc);
         String productName = CrawlerUtils.scrapStringSimpleInfo(doc, ".product__title > h1 > span", false);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".product.attribute.overview > div"));
         String primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".gallery-placeholder__image > img", "src");
         List<String> secondaryImages = getImageListFromScript(doc);
         RatingsReviews ratingsReviews = crawlRatingReviews(doc);
         boolean isAvailable = doc.select("#product-addtocart-button") != null;
         Offers offers = isAvailable ? scrapOffers(doc, internalId) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(productName)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            //.setCategories(categories)
            .setRatingReviews(ratingsReviews)
            .setOffers(offers)
            .build();
         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product__information--col") != null;
   }

   private String getInternalId(Element element) {
      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(element, ".price-box.price-final_price", "data-product-id");
      if (internalId == null) {
         internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(element, ".amxnotif-container", "data-product-id");
      }
      return internalId;
   }

   private Offers scrapOffers(Document doc, String internalId) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc, internalId);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(new Offer.OfferBuilder()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(this.SELLER_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc, String internalId) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#product-price-" + internalId, null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-container.price-final_price > span >span", null, false, ',', session);

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();

   }

   private CreditCards scrapCreditCards(Double price) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(price)
         .setFinalPrice(price)
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

   private List<String> getImageListFromScript(Document doc) {
      Element imageScript = doc.selectFirst("script:containsData(mage/gallery/gallery)");
      List<String> imagesList = new ArrayList<>();
      if (imageScript != null) {
         JSONObject imageToJson = CrawlerUtils.stringToJson(imageScript.html());
         if (imageToJson != null) {
            JSONArray imageArray = JSONUtils.getValueRecursive(imageToJson, "[data-gallery-role=gallery-placeholder].mage/gallery/gallery.data", JSONArray.class, new JSONArray());
            if (imageArray != null) {
               for (int i = 1; i < imageArray.length(); i++) {
                  String imageList = JSONUtils.getValueRecursive(imageArray, i + ".img", String.class);
                  imagesList.add(imageList);
               }
               return imagesList;
            }
         }
      }
      return null;
   }


   private RatingsReviews crawlRatingReviews(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      Integer totalReviews = computeTotalReviewsCount(doc);

      ratingReviews.setTotalRating(totalReviews);
      ratingReviews.setTotalWrittenReviews(totalReviews);
      ratingReviews.setAverageOverallRating(crawlAverageOverallRating(doc, totalReviews));
      ratingReviews.setAdvancedRatingReview(scrapAdvancedRatingReview(doc));

      return ratingReviews;
   }

   private Integer computeTotalReviewsCount(Document doc) {
      Integer totalRatings = CrawlerUtils.scrapIntegerFromHtml(doc, ".reviewCount", true, 0);
      String as = CrawlerUtils.scrapStringSimpleInfo(doc, ".reviewCount", true);

      Integer b = CommonMethods.stringPriceToIntegerPrice(as, ',',0);

      return totalRatings;
   }

   private Double crawlAverageOverallRating(Document doc, int totalReviews) {
      Elements votes = doc.select(".amount-reviews");
      double totalvalue = 0;

      if (totalReviews == 0) {
         return 0.0;
      }

      for (Element e : votes) {

         totalvalue += CrawlerUtils.scrapIntegerFromHtml(e, null, true, 0) * (5 - votes.indexOf(e));
      }

      return totalvalue / totalReviews;
   }


   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
      int star1 = 0;
      int star2 = 0;
      int star3 = 0;
      int star4 = 0;
      int star5 = 0;

      Elements stars = doc.select(".resume-label li span:first-child");
      Elements votes = doc.select(".amount-reviews");

      if (stars.size() == votes.size()) {

         for (int i = 0; i < stars.size(); i++) {

            Element starElement = stars.get(i);
            Element voteElement = votes.get(i);

            String starNumber = starElement.attr("class");
            int star = !starNumber.isEmpty() ? MathUtils.parseInt(starNumber) : 0;

            String voteNumber = CrawlerUtils.scrapStringSimpleInfo(voteElement, null, true);
            int vote = !voteNumber.isEmpty() ? MathUtils.parseInt(voteNumber) : 0;

            switch (star) {
               case 50:
                  star5 = vote;
                  break;
               case 40:
                  star4 = vote;
                  break;
               case 30:
                  star3 = vote;
                  break;
               case 20:
                  star2 = vote;
                  break;
               case 10:
                  star1 = vote;
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
