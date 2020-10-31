package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
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
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.CreditCard;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class RappiCrawler extends Crawler {

   protected final String imagesDomain = getImagesDomain();

   public RappiCrawler(Session session) {
      super(session);
      this.config.setFetcher(FetchMode.FETCHER);
   }

   protected abstract String getImagesDomain();

   @Override
   public List<Product> extractInformation(JSONObject jsonSku) throws Exception {

      List<Product> products = new ArrayList<>();

      JSONObject productJson = JSONUtils.getJSONValue(jsonSku, "product");

      if (isProductPage(productJson)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(productJson);
         String internalPid = crawlInternalPid(productJson);
         String description = crawlDescription(productJson);
         boolean available = crawlAvailability(productJson);
         String primaryImage = crawlPrimaryImage(jsonSku);
         String secondaryImages = crawlSecondaryImages(jsonSku, primaryImage);
         CategoryCollection categories = crawlCategories(productJson);
         String name = crawlName(productJson);
         List<String> eans = scrapEan(productJson);
         Offers offers = available ? scrapOffers(productJson) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setCategories(categories)
            .setDescription(description)
            .setEans(eans)
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   public static Offers scrapOffers(JSONObject jsonSku) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(jsonSku);
      List<String> sales = scrapSales(pricing);

      Offer offer = new OfferBuilder().setSellerFullName("Rappi")
         .setInternalSellerId(jsonSku.optString("store_id", null))
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setIsMainRetailer(true)
         .setSales(sales)
         .build();

      offers.add(offer);

      return offers;
   }

   public static Pricing scrapPricing(JSONObject jsonSku) throws MalformedPricingException {
      Double price = jsonSku.optDouble("price", 0D);
      Double priceFrom = jsonSku.optDouble("real_price", 0D);
      if (price == 0D || price.equals(priceFrom)) {
         price = priceFrom;
         priceFrom = null;
      }
      CreditCards creditCards = scrapCreditCards(price);

      return PricingBuilder.create()
         .setSpotlightPrice(price)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }

   public static CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      Set<Card> cards = Sets.newHashSet(
         Card.VISA,
         Card.MASTERCARD,
         Card.DINERS,
         Card.AMEX,
         Card.ELO,
         Card.SHOP_CARD
      );

      for (Card card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card.toString())
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   public static List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      if (pricing.getPriceFrom() != null && pricing.getPriceFrom() > pricing.getSpotlightPrice()) {
         BigDecimal big = BigDecimal.valueOf(pricing.getPriceFrom() / pricing.getSpotlightPrice() - 1);
         String rounded = big.setScale(2, BigDecimal.ROUND_DOWN).toString();
         sales.add('-' + rounded.replace("0.", "") + '%');
      }

      return sales;
   }

   protected List<String> scrapEan(JSONObject jsonSku) {
      List<String> eans = new ArrayList<>();
      String ean;

      if (jsonSku.has("ean")) {
         ean = jsonSku.getString("ean");

         if (ean != null && !ean.isEmpty()) {
            eans.add(ean);
         }
      }

      return eans;
   }

   /*******************************
    * Product page identification *
    *******************************/

   protected boolean isProductPage(JSONObject jsonSku) {
      return jsonSku.length() > 0;
   }

   /*******************
    * General methods *
    *******************/

   protected String crawlInternalId(JSONObject json) {
      String internalId = null;

      if (json.has("id")) {
         internalId = json.get("id").toString();
      }

      return internalId;
   }


   protected String crawlInternalPid(JSONObject json) {
      String internalPid = null;

      if (json.has("product_id")) {
         internalPid = json.getString("product_id");
      }

      return internalPid;
   }

   protected String crawlName(JSONObject json) {
      String name = null;

      if (json.has("name")) {
         name = json.getString("name");
      }

      return name;
   }

   protected boolean crawlAvailability(JSONObject json) {
      return json.has("is_available") && json.getBoolean("is_available");
   }

   protected String crawlPrimaryImage(JSONObject json) {
      String primaryImage;

      JSONArray images = JSONUtils.getJSONArrayValue(json, "images");

      if (!images.isEmpty()) {
         primaryImage = images.getString(0);
      } else {
         primaryImage = JSONUtils.getValueRecursive(json, "product.image", String.class);
      }

      return CrawlerUtils.completeUrl(primaryImage, "https", imagesDomain);
   }

   protected String crawlSecondaryImages(JSONObject json, String primaryImage) {
      JSONArray imagesArray = JSONUtils.getJSONArrayValue(json, "images");
      JSONArray resultImages = new JSONArray();

      if (imagesArray.length() > 1) {
         for (int i = 1; i < imagesArray.length(); i++) {

            String imagePath = CrawlerUtils.completeUrl(imagesArray.optString(i), "https", imagesDomain);

            if (imagePath != null && !imagePath.equals(primaryImage)) {
               resultImages.put(imagePath);
            }
         }
      }

      return imagesArray.toString();
   }

   protected CategoryCollection crawlCategories(JSONObject json) {
      CategoryCollection categories = new CategoryCollection();

      String category = JSONUtils.getStringValue(json, "category");

      if (!category.isEmpty()) {
         categories.add(category);
      }

      return categories;
   }

   protected String crawlDescription(JSONObject json) {
      StringBuilder description = new StringBuilder();

      if (json.has("description") && json.get("description") instanceof String) {
         description.append(json.getString("description"));
      }
      return description.toString();
   }
}
