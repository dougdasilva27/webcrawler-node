package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.*;

import java.util.*;

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

/**
 * Date: 12/10/2018
 *
 * @author Gabriel Dornelas
 */
public class LeroymerlinCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.leroymerlin.com.br";
   protected static String REGION;

   public LeroymerlinCrawler(Session session) {
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

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "[data-product-code]", "data-product-code");
         String internalPid = scrapInternalPid(doc);
         String name = crawlName(doc);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb-item:not(:first-child) a .name", false);
         List<String> images = crawlImages(doc);
         String primaryImage = images.isEmpty() ? null : images.get(0);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".product-header .product-text-description > div:first-child:not(.customer-service)", "[name=descricao-do-produto]", ".product-info-details"));
         Integer stock = null;

         boolean available = crawlAvailability(doc);
         Offers offers = available ? scrapOffers(doc) : new Offers();

         RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
         ratingReviewsCollection.addRatingReviews(crawlRating(doc, internalId));
         RatingsReviews ratingReviews = ratingReviewsCollection.getRatingReviews(internalId);

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
            .setSecondaryImages(images)
            .setDescription(description)
            .setStock(stock)
            .setOffers(offers)
            .setRatingReviews(ratingReviews)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private RatingsReviews crawlRating(Document doc, String internalId) {
      RatingsReviews ratingReviews = new RatingsReviews();
      JSONObject reviewSummary = new JSONObject();
      JSONObject primaryRating = new JSONObject();

      ratingReviews.setDate(session.getDate());

      String endpointRequest = assembleBazaarVoiceEndpointRequest(internalId, "caag5mZC6wgKSPPhld3GSUVaOqO46ZEpAemNYqZ38m7Yc");
      Request request = RequestBuilder.create().setUrl(endpointRequest).setCookies(cookies).build();
      JSONObject ratingReviewsEndpointResponse = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

      if (ratingReviewsEndpointResponse.has("reviewSummary")) {
         reviewSummary = ratingReviewsEndpointResponse.getJSONObject("reviewSummary");

         if (reviewSummary.has("primaryRating")) {
            primaryRating = reviewSummary.getJSONObject("primaryRating");
         }
      }
      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(getTotalRating(reviewSummary));
      ratingReviews.setAverageOverallRating(getAverageOverallRating(primaryRating));
      ratingReviews.setTotalWrittenReviews(getTotalRating(reviewSummary));

      return ratingReviews;
   }

   private Integer getTotalRating(JSONObject reviewSummary) {
      Integer total = 0;

      if (reviewSummary.has("numReviews") && reviewSummary.get("numReviews") instanceof Integer) {
         total = reviewSummary.getInt("numReviews");
      }

      return total;
   }

   private Double getAverageOverallRating(JSONObject primaryRating) {
      Double average = 0d;

      if (primaryRating.has("average") && primaryRating.get("average") instanceof Double) {
         average = primaryRating.getDouble("average");
      }

      return average;
   }

   // https://api.bazaarvoice.com/data/display/0.2alpha/product/summary?PassKey=caag5mZC6wgKSPPhld3GSUVaOqO46ZEpAemNYqZ38m7Yc&productid=88100915
   // &contentType=reviews,questions&reviewDistribution=primaryRating,recommended&rev=0&contentlocale=pt_BR

   private String assembleBazaarVoiceEndpointRequest(String skuInternalId, String bazaarVoiceEnpointPassKey) {
      StringBuilder request = new StringBuilder();

      request.append("https://api.bazaarvoice.com/data/display/0.2alpha/product/summary?");
      request.append("&Passkey=" + bazaarVoiceEnpointPassKey);
      request.append("&productid=" + skuInternalId);
      request.append("&contentType=reviews,questions");
      request.append("&reviewDistribution=primaryRating,recommended");
      request.append("&rev=0");
      request.append("&contentlocale=pt_BR");

      return request.toString();
   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".product-code").isEmpty();
   }

   private String scrapInternalPid(Document doc) {
      String internalPid = null;

      Element internalPidElement = doc.selectFirst(".product-code");
      if (internalPidElement != null) {
         String text = internalPidElement.text();

         if (text.contains(".")) {
            internalPid = CommonMethods.getLast(text.split("\\.")).trim();
         } else if (text.contains("digo")) {
            internalPid = CommonMethods.getLast(text.split("digo")).trim();
         } else {
            internalPid = text.trim();
         }

         internalPid = internalPid.split(",")[0];
      }

      return internalPid;
   }

   private String crawlName(Document document) {
      String name = null;
      Element nameElement = document.selectFirst(".product-title");

      if (nameElement != null) {
         name = nameElement.ownText().trim();
      }

      return name;
   }

   private List<String> crawlImages(Document doc) {
      List<String> images = new ArrayList<>();

      Element divImages = doc.selectFirst(".product-carousel .carousel[data-items]");
      if (divImages != null) {
         JSONArray imagesArray = new JSONArray(divImages.attr("data-items"));

         for (Object o : imagesArray) {
            JSONObject imageJson = (JSONObject) o;

            // in this case the image is a video thumbnail
            if (imageJson.has("youtubeId") && imageJson.get("youtubeId") instanceof String && !imageJson.getString("youtubeId").trim().isEmpty()) {
               continue;
            }

            if (imageJson.has("shouldLoadImageZoom") && imageJson.getBoolean("shouldLoadImageZoom") && imageJson.has("zoomUrl")
               && !imageJson.get("zoomUrl").toString().trim().isEmpty()) {
               images.add(imageJson.get("zoomUrl").toString());
            } else if (imageJson.has("url") && !imageJson.get("url").toString().trim().isEmpty()) {
               images.add(imageJson.get("url").toString());
            } else if (imageJson.has("thumb") && !imageJson.get("thumb").toString().trim().isEmpty()) {
               images.add(imageJson.get("thumb").toString());
            }
         }
      }

      return images;
   }

   private boolean crawlAvailability(Document doc) {
      return !doc.select(".product-purchase-buttons .buy-button[data-button=ecommerce]:not(.disabled), "
         + ".product-purchase-buttons .buy-button[data-button=pickupInStore]:not([disabled])").isEmpty();
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Pricing pricing = scrapPricing(doc);

      String sellerNameLower = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "div.price-tag-wrapper", "data-shop-name").toLowerCase(Locale.ROOT);

      boolean isMainRetailer = sellerNameLower.contains("leroy");
      String sellerFullname = isMainRetailer ? "leroy merlin " + REGION : sellerNameLower;

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(sellerFullname)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(isMainRetailer)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      String spotlightPriceStr = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "div.price-tag-wrapper", "data-to-price-integers");
      spotlightPriceStr += "." + CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "div.price-tag-wrapper", "data-to-price-decimals");
      String priceFromStr = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "div.price-tag-wrapper", "data-from-price-integers");
      priceFromStr += "." + CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "div.price-tag-wrapper", "data-from-price-decimals");

      Double spotlightPrice = MathUtils.parseDoubleWithDot(spotlightPriceStr);
      Double priceFrom = null;

      if(!priceFromStr.contains("null")){
         priceFrom = MathUtils.parseDoubleWithDot(priceFromStr);
      }

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
      Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString(), Card.DINERS.toString(), Card.SHOP_CARD.toString());

      Installments installments = scrapInstallments(doc);
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

   public Installments scrapInstallments(Document doc) throws MalformedPricingException {
      Installments installments = new Installments();

      String jsonPrices = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "div [data-postal-code]", "data-skus");
      JSONArray jsonArr = CrawlerUtils.stringToJsonArray(jsonPrices);

      if (!jsonArr.isEmpty()) {
         JSONObject json = jsonArr.optJSONObject(0);

         if(json.has("installmentsAmount")){
            Integer installment = json.optInt("installmentsAmount");
            String value = json.optString("installmentsValue");

            installments.add(Installment.InstallmentBuilder.create()
               .setInstallmentNumber(installment)
               .setInstallmentPrice(MathUtils.parseDoubleWithComma(value))
               .build());
         }

         if (json.has("brandedInstallmentsAmount")) {
            Integer brandedInstallment = json.optInt("brandedInstallmentsAmount");
            String brandedValue = json.optString("brandedInstallmentsValue");

            installments.add(Installment.InstallmentBuilder.create()
               .setInstallmentNumber(brandedInstallment)
               .setInstallmentPrice(MathUtils.parseDoubleWithComma(brandedValue))
               .build());
         }
      }

      return installments;
   }
}
