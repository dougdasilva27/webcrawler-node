package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
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
import models.Offer;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.CreditCard;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

public abstract class RappiCrawler extends Crawler {

   protected final String imagesDomain = getImagesDomain();

   public RappiCrawler(Session session) {
      super(session);
      this.config.setFetcher(FetchMode.FETCHER);
   }

   protected abstract String getImagesDomain();

   @Override
   public List<Product> extractInformation(JSONObject jsonSku) throws Exception {
      super.extractInformation(jsonSku);
      List<Product> products = new ArrayList<>();
      String productUrl = session.getOriginalURL();

      if (isProductPage(jsonSku)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(jsonSku);
         String internalPid = crawlInternalPid(jsonSku);
         String description = crawlDescription(jsonSku);
         boolean available = crawlAvailability(jsonSku);
         String primaryImage = crawlPrimaryImage(jsonSku);
         String secondaryImages = crawlSecondaryImages(jsonSku, primaryImage);
         CategoryCollection categories = crawlCategories(jsonSku);
         String name = crawlName(jsonSku);
         List<String> eans = scrapEan(jsonSku);
         Offers offers = available ? scrapOffers(jsonSku) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
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

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(),
            Card.MASTERCARD.toString(),
            Card.DINERS.toString(),
            Card.AMEX.toString(),
            Card.ELO.toString(),
            Card.SHOP_CARD.toString());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
               .setBrand(card)
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
      String ean = null;

      if (jsonSku.has("ean")) {
         ean = jsonSku.getString("ean");

         if (ean != null) {
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
      String primaryImage = null;

      if (json.has("image") && json.get("image") instanceof String) {
         primaryImage = CrawlerUtils.completeUrl(json.getString("image"), "https", imagesDomain);
      }

      return primaryImage;
   }

   protected String crawlSecondaryImages(JSONObject json, String primaryImage) {
      JSONArray imagesArray = new JSONArray();

      if (json.has("store_id") && !json.isNull("store_id") && json.has("product_id") && !json.isNull("product_id")) {
         JSONArray jsonImagesArray = crawlProductImagesFromApi(json.get("product_id").toString(), json.get("store_id").toString());

         for (Object obj : jsonImagesArray) {
            if (obj instanceof JSONObject) {
               JSONObject imageObj = (JSONObject) obj;

               if (imageObj.has("name") && imageObj.get("name") instanceof String) {
                  String secondaryImage = CrawlerUtils.completeUrl(imageObj.getString("name"), "https", imagesDomain);

                  if (!secondaryImage.equals(primaryImage)) {
                     imagesArray.put(CrawlerUtils.completeUrl(imageObj.getString("name"), "https", imagesDomain));
                  }
               }
            }
         }
      }

      return imagesArray.toString();
   }

   protected JSONArray crawlProductImagesFromApi(String productId, String storeId) {
      return new JSONArray();
   }

   protected CategoryCollection crawlCategories(JSONObject json) {
      CategoryCollection categories = new CategoryCollection();

      if (json.has("categories")) {
         JSONArray shelfList = json.getJSONArray("categories");

         for (Object o : shelfList) {
            JSONObject cat = (JSONObject) o;

            if (cat.has("category_name")) {
               categories.add(cat.getString("category_name"));
            }
         }
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
