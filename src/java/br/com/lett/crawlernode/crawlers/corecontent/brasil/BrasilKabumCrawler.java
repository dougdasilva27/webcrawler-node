package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BrasilKabumCrawler extends Crawler {

   private static final String HOME_PAGE = "http://www.kabum.com.br";
   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString());

   private static final String SELLER_NAME = "Kabum";

   public BrasilKabumCrawler(Session session) {
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
      JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "#__NEXT_DATA__", null, null, false, true);

      if (isProductPage(json)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject productData = JSONUtils.getValueRecursive(json, "props.initialProps.pageProps.productData", JSONObject.class);

         if (productData != null && !productData.isEmpty()) {

            String internalId = productData.optString("code");
            String internalPid = internalId;
            String name = productData.optString("name");
            List<String> images = getImages(productData);
            String primaryImage = !images.isEmpty() ? images.remove(0) : null;
            String description = getDescription(productData);
            boolean available = productData.optBoolean("available");
            Offers offers = available ? scrapOffers(productData) : new Offers();
            RatingsReviews ratingsReviews = scrapRatingAndReviews(productData, internalId);

            // Creating the product
            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(images)
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

   private List<String> getImages(JSONObject json) {
      JSONArray imageArray = json.optJSONArray("photos");
      return imageArray.toList().stream().map(Object::toString).collect(Collectors.toList());
   }

   private String getDescription(JSONObject productData) {
      StringBuilder builderDescription = new StringBuilder();
      String description = productData.optString("description");
      String technicalInformation = productData.optString("technicalInformation");
      if (description != null) {
         builderDescription.append(description);
      }
      if (technicalInformation != null) {
         builderDescription.append("\\n").append(technicalInformation);
      }

      return builderDescription.toString();

   }


   private Offers scrapOffers(JSONObject productData) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productData);
      List<String> sales = scrapSales(pricing);

      boolean isMainRetailer = true;

      String seller = productData.optString("sellerName");
      if (seller != null && !seller.isEmpty()) {
         isMainRetailer = seller.equalsIgnoreCase(SELLER_NAME);

      } else {
         seller = SELLER_NAME;
      }

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(seller)
         .setIsBuybox(false)
         .setIsMainRetailer(isMainRetailer)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();
      String salesOnJson = CrawlerUtils.calculateSales(pricing);
      if (salesOnJson != null) {
         sales.add(salesOnJson);

      }
      return sales;
   }

   private Pricing scrapPricing(JSONObject productInfo) throws MalformedPricingException {

      JSONObject priceInfo = productInfo.optJSONObject("priceDetails");

      Double priceFrom = JSONUtils.getDoubleValueFromJSON(productInfo, "oldPrice", true);
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(productInfo, "price", true);
      CreditCards creditCards = scrapCreditCards(spotlightPrice, productInfo);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(priceInfo != null ? JSONUtils.getDoubleValueFromJSON(priceInfo, "discountPrice", true) : null)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice, JSONObject productData) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();

      JSONArray installmentsArray = productData.optJSONArray("parcels");

      if (installmentsArray != null && !installmentsArray.isEmpty()) {
         for (Object obj : installmentsArray) {
            JSONObject parcels = (JSONObject) obj;

            installments.add(Installment.InstallmentBuilder.create()
               .setInstallmentNumber(parcels.optInt("parcelNumber"))
               .setInstallmentPrice(parcels.optDouble("value"))
               .build());
         }
      } else {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }


      for (String brand : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(brand)
            .setIsShopCard(false)
            .setInstallments(installments)
            .build());
      }

      return creditCards;
   }


   private boolean isProductPage(JSONObject json) {
      String page = json.optString("page");
      return page.equals("/produto");
   }


   private RatingsReviews scrapRatingAndReviews(JSONObject json, String internalId) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = CrawlerUtils.getIntegerValueFromJSON(json, "ratingNumber", 0);
      Double avgRating = CrawlerUtils.getDoubleValueFromJSON(json, "rating", true, null);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating != null ? avgRating : 0D);

      ratingReviews.setInternalId(internalId);


      return ratingReviews;

   }

}
