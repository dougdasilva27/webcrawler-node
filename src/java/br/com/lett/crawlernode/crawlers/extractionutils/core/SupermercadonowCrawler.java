package br.com.lett.crawlernode.crawlers.extractionutils.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

public abstract class SupermercadonowCrawler extends Crawler {

   private static final String HOME_PAGE = "https://supermercadonow.com/";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());
   protected String loadUrl = getLoadUrl();
   protected String sellerFullName = getSellerFullName();

   protected abstract String getLoadUrl();

   protected abstract String getSellerFullName();

   private Map<String, String> headers = new HashMap<>();

   public SupermercadonowCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   protected Object fetch() {
      String pathUrl = this.session.getOriginalURL();
      String[] urlPath = pathUrl.split("/");
      pathUrl = urlPath[urlPath.length - 1];

      String apiUrl = "https://supermercadonow.com/api/v2/stores/" + loadUrl + "/product/" + pathUrl;

      headers.put("X-SNW-Token", "XLBhhbP1YEkB2tL61wkX163Dqm9iIDpx");
      headers.put("Accept", "text/json");

      Request request = RequestBuilder.create().setUrl(apiUrl).setCookies(cookies).setHeaders(headers).build();
      return CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
   }

   @Override
   public List<Product> extractInformation(JSONObject jsonSku) throws Exception {
      super.extractInformation(jsonSku);

      List<Product> products = new ArrayList<>();

      if (jsonSku.length() > 0) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(jsonSku);
         String internalPid = crawlInternalPid(jsonSku);
         CategoryCollection categories = crawlCategories(internalId);
         String description = crawlDescription(jsonSku);
         boolean available = crawlAvailability(jsonSku);
         String primaryImage = crawlPrimaryImage(jsonSku);
         String name = crawlName(jsonSku);
         String secondaryImages = crawlSecondaryImages(jsonSku, primaryImage);
         Offers offers = available ? scrapOffers(jsonSku) : new Offers();

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
               .setOffers(offers)
               .build();

         products.add(product);


      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private String crawlInternalId(JSONObject json) {
      String internalId = null;

      if (json.has("product_store_id")) {
         internalId = json.get("product_store_id").toString();
      }

      return internalId;
   }


   private String crawlInternalPid(JSONObject json) {
      String internalPid = null;

      if (json.has("slug")) {
         internalPid = json.get("slug").toString().split("-")[0];
      }

      return internalPid;
   }

   private String crawlName(JSONObject json) {
      String name = null;

      if (json.has("name")) {
         name = json.getString("name");
      }

      return name;
   }

   private boolean crawlAvailability(JSONObject json) {
      return json.has("in_stock") &&
            json.get("in_stock") instanceof Boolean &&
            json.getBoolean("in_stock");
   }

   private Offers scrapOffers(JSONObject jsonSku) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();

      Double spotlightPrice = CrawlerUtils.getDoubleValueFromJSON(jsonSku, "price", true, false);
      Double originalPrice = CrawlerUtils.getDoubleValueFromJSON(jsonSku, "original_price", true, false);
      if (spotlightPrice != null) {
         Double priceFrom = originalPrice != null && originalPrice > spotlightPrice ? originalPrice : null;

         Pricing pricing = PricingBuilder.create()
               .setPriceFrom(priceFrom)
               .setSpotlightPrice(spotlightPrice)
               .setCreditCards(scrapCreditCards(spotlightPrice))
               .setBankSlip(BankSlipBuilder.create()
                     .setFinalPrice(spotlightPrice)
                     .build())
               .build();

         List<String> sales = new ArrayList<>();
         if (priceFrom != null) {
            Double percentage = MathUtils.normalizeNoDecimalPlaces((spotlightPrice / priceFrom) * 100d);
            sales.add(percentage.intValue() + "% OFF");
         }

         offers.add(OfferBuilder.create()
               .setIsMainRetailer(true)
               .setSellerFullName(sellerFullName)
               .setUseSlugNameAsInternalSellerId(true)
               .setIsBuybox(false)
               .setMainPagePosition(1)
               .setPricing(pricing)
               .setSales(sales)
               .build());
      }

      return offers;
   }

   private CreditCards scrapCreditCards(Double price) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(InstallmentBuilder.create()
            .setFinalPrice(price)
            .setInstallmentNumber(1)
            .setInstallmentPrice(price)
            .build());

      for (String flag : cards) {
         creditCards.add(CreditCardBuilder.create()
               .setBrand(flag)
               .setIsShopCard(false)
               .setInstallments(installments)
               .build());
      }

      return creditCards;
   }

   private String crawlPrimaryImage(JSONObject json) {
      String primaryImage = null;

      if (json.has("all_images")) {
         JSONArray images = json.getJSONArray("all_images");

         if (images.length() > 0) {
            primaryImage = CrawlerUtils.completeUrl(images.get(0).toString(), "https", "d3o3bdzeq5san1.cloudfront.net");
         }
      }

      return primaryImage;
   }

   private String crawlSecondaryImages(JSONObject json, String primaryImage) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      if (json.has("all_images")) {
         JSONArray images = json.getJSONArray("all_images");

         for (Object o : images) {
            String image = CrawlerUtils.completeUrl(o.toString(), "https", "d3o3bdzeq5san1.cloudfront.net");

            if (!image.equalsIgnoreCase(primaryImage)) {
               secondaryImagesArray.put(image);
            }
         }
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   private CategoryCollection crawlCategories(String internalId) {
      CategoryCollection categories = new CategoryCollection();

      String categoryUrl = "https://supermercadonow.com/api/products/" + internalId + "/category-tree";
      Request request = RequestBuilder.create().setUrl(categoryUrl).setCookies(cookies).setHeaders(headers).build();

      JSONArray categoriesArray = CrawlerUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());
      for (Object o : categoriesArray) {
         JSONObject categoryObject = (JSONObject) o;

         if (categoryObject.has("name") && !categoryObject.isNull("name")) {
            String categoryName = categoryObject.get("name").toString();

            if (!categoryName.equalsIgnoreCase("todos os produtos")) {
               categories.add(categoryName);
            }
         }
      }

      return categories;
   }


   private String crawlDescription(JSONObject json) {
      StringBuilder description = new StringBuilder();

      if (json.has("description") && json.get("description") instanceof String) {
         description.append(json.getString("description"));
      }

      return description.toString();
   }
}
