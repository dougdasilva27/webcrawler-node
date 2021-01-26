package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.YourreviewsRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
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


public class BrasilCasaeconstrucaoCrawler extends Crawler {

   public BrasilCasaeconstrucaoCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   private static final String HOME_PAGE = "https://www.cec.com.br/";

   private static final String SELLER_FULL_NAME = "Casa e Construcao Brasil";
   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString(),
      Card.DINERS.toString());


   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
   }

   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(doc);
         String internalPid = crawlInternalPid(doc);
         String name = crawlName(doc);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li:not(:first-child) > a", false);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#productDetail"));
         JSONArray imagesArray = CrawlerUtils.selectJsonArrayFromHtml(doc, "script", "window['Images']=", ";", true, false);
         List<String> images = CrawlerUtils.scrapImagesListFromJSONArray(imagesArray, "Standard", null, "https", "carrinho.cec.com.br", session);
         String primaryImage = images != null && !images.isEmpty() ? images.remove(0) : null;
         RatingsReviews ratingReviews = crawlRating(internalId);
         boolean available = crawlAvailability(doc);

         Offers offers = available ? scrapOffers(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
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

   private RatingsReviews crawlRating(String internalId) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      YourreviewsRatingCrawler yourReviews = new YourreviewsRatingCrawler(session, cookies, logger);

      Document docRating = yourReviews.crawlPageRatingsFromYourViews(internalId, "53f7df57-db2b-4521-b829-617abf75405d", dataFetcher);

      Integer totalNumOfEvaluations = getTotalNumOfRatingsFromYourViews(docRating, "p[itemprop=\"count\"]");

      Double avgRating = getTotalAvgRatingFromYourViews(docRating, ".rating-number .yv-count-stars1");

      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);

      return ratingReviews;
   }

   private Double getTotalAvgRatingFromYourViews(Document docRating, String cssSelector) {
      Double avgRating = 0d;
      Element rating = docRating.select(cssSelector).first();

      if (rating != null) {
         avgRating = MathUtils.parseDoubleWithDot(rating.text().trim());
      }

      return avgRating;
   }

   private Integer getTotalNumOfRatingsFromYourViews(Document doc, String cssSelector) {
      Integer totalRating = 0;
      Element totalRatingElement = doc.select(cssSelector).first();

      if (totalRatingElement != null) {
         String totalText = totalRatingElement.text().replaceAll("[^0-9]", "").trim();

         if (!totalText.isEmpty()) {
            totalRating = Integer.parseInt(totalText);
         }
      }

      return totalRating;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".product-identification").isEmpty();
   }

   private String crawlInternalId(Document document) {
      String internalId = null;

      JSONObject skuJson = CrawlerUtils.selectJsonFromHtml(document, "script", "var google_tag_params = ", ";", false, false);
      if (skuJson.has("ecomm_prodid")) {
         internalId = skuJson.getString("ecomm_prodid");
      }

      return internalId;
   }

   private String crawlInternalPid(Document document) {
      String internalPid = null;
      String[] internalIdArray = CrawlerUtils.scrapStringSimpleInfo(document, ".sku", true).split("Cód: ");
      if (internalIdArray.length > 1) {
         internalPid = internalIdArray[1];
      }

      return internalPid;
   }

   private String crawlName(Document doc) {
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1[itemprop=name]", true);
      String brand = CrawlerUtils.scrapStringSimpleInfo(doc, "h2[itemprop=brand]", true);

      return name != null && brand != null ? brand + " " + name : null;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(doc, pricing);

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

   private List<String> scrapSales(Document doc, Pricing pricing) {
      List<String> sales = new ArrayList<>();
      if (scrapSalePromo(doc) != null) {
         sales.add(scrapSalePromo(doc));
      }
      if (scrapSaleDiscount(pricing) != null) {
         sales.add(scrapSaleDiscount(pricing));
      }

      return sales;
   }

   private String scrapSalePromo(Document doc) {

      return CrawlerUtils.scrapStringSimpleInfo(doc, "#Body_Body_divPromotionInfo > p", true);
   }


   private String scrapSaleDiscount(Pricing pricing) {

      return CrawlerUtils.calculateSales(pricing);
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-price .price strong", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-campaign > span", null, false, ',', session);
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);
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

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(".product-monthly-payment", doc, false);
      if (!pair.isAnyValueNull()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(pair.getFirst())
            .setInstallmentPrice(MathUtils.normalizeTwoDecimalPlaces(((Float) pair.getSecond()).doubleValue()))
            .build());
      } else {
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

   private boolean crawlAvailability(Document doc) {
      return doc.selectFirst("[id~=btnAddBasket]") != null;
   }

}
