package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.YourreviewsRatingCrawler;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;


public class BrasilHavanCrawler extends Crawler {

   private final String HOME_PAGE = "https://www.havan.com.br";
   private static final String SELLER_FULL_NAME = "havan";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilHavanCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-add-form form", "data-product-sku");
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".price-box.price-final_price", "data-product-id");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".page-title .base", false);
         boolean available = doc.selectFirst("#product-addtocart-button") != null;

         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".fotorama__stage .fotorama__stage__frame", Arrays.asList("href"), "https", "https://www.havan.com.br/");
         List<String> secondaryImages = getSecondaryImages(doc);
         RatingsReviews ratingReviews = scrapRatingReviews(doc, internalPid);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb > ul li a");
         String description =
            CrawlerUtils.scrapSimpleDescription(doc,
               Arrays.asList(".product.info.detailed .data .item", ".product.attribute.description .value p", "#tab-label-ficha-tecnica-title", "#ficha-tecnica .striped-table tbody"));

         Offers offers = available ? scrapOffer(doc) : new Offers();

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
            .setRatingReviews(ratingReviews)
            .setOffers(offers)
            .build();

         products.add(product);


      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document document) {
      return document.selectFirst(".column.main") != null;
   }

   private List<String> getSecondaryImages(Document doc) {
      List<String> images = new ArrayList<>();
      String script = CrawlerUtils.scrapScriptFromHtml(doc, ".product.media script[type=\"text/x-magento-init\"]");
      JSONArray jsonArray = script != null ? CrawlerUtils.stringToJsonArray(script) : null;
      JSONArray dataImage = jsonArray != null ? JSONUtils.getValueRecursive(jsonArray, "0.[data-gallery-role=gallery-placeholder].Hibrido_FastProductImages/js/gallery/custom_gallery.data", JSONArray.class) : new JSONArray();

      int n = 0;
      for (Object o : dataImage) {
         if (o instanceof JSONObject) {
            JSONObject jsonWithImages = (JSONObject) o;
            String image = jsonWithImages.optString("img").replace("/", "");
            if (n != 0) {
               images.add(image);
            }
         }
         n++;
      }

      return images;


   }

   private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = new ArrayList<>();

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

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".old-price .price-wrapper .price", null, false, ',', session);
      Double spotlightPrice = 0D;
      if (priceFrom != null) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".special-price .price", null, false, ',', session);
      } else {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-container .price", null, false, ',', session);
      }
      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();


   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private RatingsReviews scrapRatingReviews(Document doc, String internalPid) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      YourreviewsRatingCrawler yourReviews =
         new YourreviewsRatingCrawler(session, cookies, logger, "5e9bcbac-4433-4a16-992a-1bbd2d62067c", this.dataFetcher);

      Document docRating = yourReviews.crawlPageRatingsFromYourViews(internalPid, "5e9bcbac-4433-4a16-992a-1bbd2d62067c", this.dataFetcher);


      Integer totalNumOfEvaluations = yourReviews.getTotalNumOfRatingsFromYourViews(docRating);
      Double avgRating = yourReviews.getTotalAvgRatingFromYourViews(docRating);
      AdvancedRatingReview advancedRatingReview = yourReviews.getTotalStarsFromEachValue(internalPid);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);


      return ratingReviews;
   }

}
