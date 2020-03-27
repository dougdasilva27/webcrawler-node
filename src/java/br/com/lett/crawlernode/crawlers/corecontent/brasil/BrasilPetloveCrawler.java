package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

/**
 * date: 27/03/2018
 * 
 * @author gabriel
 *
 */

public class BrasilPetloveCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.petlove.com.br/";
   private static final String SELLER_FULL_NAME = "petlove";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilPetloveCrawler(Session session) {
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
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.dataLayer.push(", ");", false, false);
         JSONObject productJson = JSONUtils.getJSONValue(json, "info");
         String internalPid = crawlInternalPid(productJson);
         CategoryCollection categories = crawlCategories(doc);
         String description = crawlDescription(doc);
         Integer stock = null;


         JSONArray arraySkus = JSONUtils.getJSONArrayValue(productJson, "variants");

         for (Object obj : arraySkus) {
            if (obj instanceof JSONObject) {
               JSONObject jsonSku = (JSONObject) obj;

               String internalId = JSONUtils.getStringValue(jsonSku, "sku");
               String name = crawlName(jsonSku);
               String primaryImage = crawlPrimaryImage(jsonSku);
               String secondaryImages = crawlSecondaryImages(jsonSku);
               RatingsReviews ratingReviews = crawlRating(doc);
               boolean available = (jsonSku.has("in_stock") && jsonSku.get("in_stock") instanceof Boolean) && jsonSku.getBoolean("in_stock");
               Offers offers = available ? scrapOffers(jsonSku, doc) : new Offers();

               // Creating the product
               Product product = ProductBuilder.create()
                     .setUrl(session.getOriginalURL())
                     .setRatingReviews(ratingReviews)
                     .setInternalId(internalId)
                     .setInternalPid(internalPid)
                     .setName(name)
                     .setCategory1(categories.getCategory(0))
                     .setCategory2(categories.getCategory(1))
                     .setCategory3(categories.getCategory(2))
                     .setPrimaryImage(primaryImage)
                     .setSecondaryImages(secondaryImages)
                     .setDescription(description)
                     .setStock(stock)
                     .setOffers(offers)
                     .build();

               products.add(product);
            }
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("#product") != null;
   }

   private String crawlInternalPid(JSONObject json) {
      String internalPid = null;

      if (json.has("master") && json.get("master") instanceof JSONObject) {
         JSONObject master = json.getJSONObject("master");

         if (master.has("product_sku") && master.get("product_sku") instanceof String) {
            internalPid = master.getString("product_sku");
         }
      }

      return internalPid;
   }

   private RatingsReviews crawlRating(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = getTotalNumOfRatings(doc);
      Double avgRating = getTotalAvgRating(doc);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);

      return ratingReviews;
   }

   private Integer getTotalNumOfRatings(Document doc) {
      Integer totalRating = 0;
      Element rating = doc.select(".box-rating [itemprop=reviewCount]").first();

      if (rating != null) {
         String votes = rating.text().replaceAll("[^0-9]", "").trim();

         if (!votes.isEmpty()) {
            totalRating = Integer.parseInt(votes);
         }
      }

      return totalRating;
   }

   /**
    * 
    * @param document
    * @return
    */
   private Double getTotalAvgRating(Document docRating) {
      Double avgRating = 0d;
      Element rating = docRating.select(".rating-avg").first();

      if (rating != null) {
         String text = rating.ownText();

         if (!text.isEmpty()) {
            avgRating = Double.parseDouble(text);
         }
      }

      return avgRating;
   }

   private String crawlName(JSONObject jsonSku) {
      StringBuilder name = new StringBuilder();

      if (jsonSku.has("name") && jsonSku.get("name") instanceof String) {
         name.append(jsonSku.getString("name"));

         if (jsonSku.has("label_name") && !jsonSku.isNull("label_name")) {
            name.append(" ");
            name.append(jsonSku.get("label_name"));
         }

         if (jsonSku.has("original_short_name") && jsonSku.get("original_short_name") instanceof String) {
            String shortName = jsonSku.getString("original_short_name");

            if (!name.toString().toLowerCase().contains(shortName.toLowerCase())) {
               name.append(" ");
               name.append(shortName);
            }
         }
      }

      return name.toString().trim();
   }

   private String crawlPrimaryImage(JSONObject skuJson) {
      String primaryImage = null;

      if (skuJson.has("images") && skuJson.get("images") instanceof JSONArray && skuJson.getJSONArray("images").length() > 0) {
         JSONObject image = skuJson.getJSONArray("images").getJSONObject(0);

         if (image.has("fullhd_url") && image.get("fullhd_url").toString().startsWith("http")) {
            primaryImage = image.get("fullhd_url").toString();
         } else if (image.has("large_url") && image.get("large_url").toString().startsWith("http")) {
            primaryImage = image.get("large_url").toString();
         } else if (image.has("small_url") && image.get("small_url").toString().startsWith("http")) {
            primaryImage = image.get("small_url").toString();
         }
      }

      return primaryImage;
   }

   private String crawlSecondaryImages(JSONObject skuJson) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      if (skuJson.has("images") && skuJson.get("images") instanceof JSONArray && skuJson.getJSONArray("images").length() > 1) {
         JSONArray images = skuJson.getJSONArray("images");

         for (int i = 1; i < images.length(); i++) { // starts with index 1, because the first image is
                                                     // the primary image
            JSONObject image = images.getJSONObject(i);

            if (image.has("fullhd_url") && image.get("fullhd_url").toString().startsWith("http")) {
               secondaryImagesArray.put(image.get("fullhd_url").toString());
            } else if (image.has("large_url") && image.get("large_url").toString().startsWith("http")) {
               secondaryImagesArray.put(image.get("large_url").toString());
            } else if (image.has("small_url") && image.get("small_url").toString().startsWith("http")) {
               secondaryImagesArray.put(image.get("small_url").toString());
            }
         }
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   private CategoryCollection crawlCategories(Document document) {
      CategoryCollection categories = new CategoryCollection();
      Elements elementCategories = document.select(".list-category li a > span[itemprop]");

      for (Element e : elementCategories) {
         String cat = e.ownText().trim();

         if (!cat.isEmpty()) {
            categories.add(cat);
         }
      }

      return categories;
   }


   private String crawlDescription(Document doc) {
      StringBuilder description = new StringBuilder();

      Element elementShortdescription = doc.select(".product-resume").first();

      if (elementShortdescription != null) {
         description.append(elementShortdescription.html());
      }

      Element elementDescription = doc.select(".tab-content").first();

      if (elementDescription != null) {
         description.append(elementDescription.html());
      }

      return description.toString();
   }


   private Offers scrapOffers(JSONObject skuJson, Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      List<String> sales = scrapSales(doc);
      Pricing pricing = scrapPricing(skuJson);

      offers.add(OfferBuilder.create()
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

   private List<String> scrapSales(Document doc) {
      List<String> sales = new ArrayList<>();

      Element salesOneElement = doc.selectFirst(".product-info .flag.product-discount-flag.product-discount-flag-price");
      String firstSales = salesOneElement != null ? salesOneElement.text() : null;

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }
      return sales;
   }


   private Pricing scrapPricing(JSONObject skuJson) throws MalformedPricingException {
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(skuJson, "list_price", false);
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(skuJson, "display_price", false);
      CreditCards creditCards = scrapCreditCards(skuJson, spotlightPrice);

      return PricingBuilder.create()
            .setPriceFrom(priceFrom)
            .setSpotlightPrice(spotlightPrice)
            .setCreditCards(creditCards)
            .build();
   }

   private CreditCards scrapCreditCards(JSONObject skuJson, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(skuJson);
      if (installments.getInstallments().isEmpty()) {
         installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(1)
               .setInstallmentPrice(spotlightPrice)
               .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCardBuilder.create()
               .setBrand(card)
               .setInstallments(installments)
               .setIsShopCard(false)
               .build());
      }

      return creditCards;
   }

   public Installments scrapInstallments(JSONObject skuJson) throws MalformedPricingException {
      Installments installments = new Installments();

      String installmentsCard = JSONUtils.getStringValue(skuJson, "display_best_installment");

      if (installmentsCard != null) {

         String installmentCard = installmentsCard;
         int ou = installmentCard.contains("ou") ? installmentCard.indexOf("ou") : null;
         int x = installmentCard.contains("x") ? installmentCard.lastIndexOf("x") : null;
         int installment = Integer.parseInt(installmentCard.substring(ou, x).replaceAll("[^0-9]", "").trim());

         String valueCard = installmentsCard;
         int de = valueCard.contains("R$") ? valueCard.indexOf("R$") : null;
         Double value = MathUtils.parseDoubleWithComma(valueCard.substring(de));

         installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(installment)
               .setInstallmentPrice(value)
               .build());
      }
      return installments;
   }
}