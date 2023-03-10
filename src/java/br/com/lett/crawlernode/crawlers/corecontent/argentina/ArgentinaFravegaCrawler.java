package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import java.util.*;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;

public class ArgentinaFravegaCrawler extends Crawler {

   private final String HOME_PAGE = "https://www.fravega.com/";
   private static final String SELLER_FULL_NAME = "Frávega";
   private static final Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString());

   protected String getPostalCode() {
      return session.getOptions().optString("postal_code");
   }

   public ArgentinaFravegaCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSON);
   }

   @Override
   protected Response fetchResponse() {
      String url = scrapProductUrl();
      Map<String, String> headers = new HashMap<>();
      headers.put("authority", "www.fravega.com");
      headers.put("referer", "https://www.fravega.com/");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();

      return this.dataFetcher.get(session, request);
   }

   private @NotNull String scrapProductUrl() {
      String[] urlParts = session.getOriginalURL().split("/");
      String[] productSlugParts = urlParts[urlParts.length - 1].split("-");

      String productSlug = urlParts[urlParts.length - 1];
      String sku = productSlugParts[productSlugParts.length - 1];

      String[] slugParts = Arrays.copyOf(productSlugParts, productSlugParts.length - 1);
      String slug = String.join("-", slugParts);

      return "https://www.fravega.com/_next/data/F6xWugkgMp7lt6cbNmjb3/es-AR/p/" + productSlug + ".json?sku=" + sku + "&slug=" + slug + "&productSlug=" + productSlug;
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      List<Product> products = new ArrayList<>();
      String[] urlParts = session.getOriginalURL().split("/");
      String[] productSlugParts = urlParts[urlParts.length - 1].split("-");

      String sku = productSlugParts[productSlugParts.length - 1];
      JSONObject productJson = JSONUtils.getValueRecursive(json, "pageProps.__APOLLO_STATE__.ROOT_QUERY.sku({\"code\":\"" + sku + "\"})", JSONObject.class);

      if(productJson != null) {
         String name = JSONUtils.getValueRecursive(productJson, "item.title", String.class);
         String internalPid = JSONUtils.getValueRecursive(productJson, "item.id", String.class);
         String internalId = productJson.optString("code");
         JSONArray imagesJson = JSONUtils.getValueRecursive(productJson, "item.images", JSONArray.class);
         List<String> images = formatImages(imagesJson);
         String primaryImage = images != null && !images.isEmpty() ? images.remove(0) : null;
         String description = scrapDescription(productJson.optJSONObject("item"));
         CategoryCollection categories = scrapCategories(productJson);
         boolean available = JSONUtils.getValueRecursive(productJson.optJSONObject("stock({\"postalCode\":\"" + getPostalCode() + "\"})"), "availability", Boolean.class);
         Offers offers = available ? scrapOffers(productJson) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setCategories(categories)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private List<String> formatImages(JSONArray images) throws OfferException, MalformedPricingException {
      List<String> formatedImages = new ArrayList<>();
      for (Object image:images) {
         formatedImages.add("https://images.fravega.com/f500/" + image.toString());
      }
      return formatedImages;
   }

   private Offers scrapOffers(JSONObject product) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(product);
      List<String> sales = scrapSales(product);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setSales(sales)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject product) throws MalformedPricingException {
      JSONObject pricingObject = JSONUtils.getValueRecursive(product, "pricing({\"channel\":\"fravega-ecommerce\"}).0", JSONObject.class);
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(pricingObject, "salePrice", true);
      Double priceFrom =  JSONUtils.getDoubleValueFromJSON(pricingObject, "listPrice", true);
      if(spotlightPrice == null) { spotlightPrice = (double) 0; };

      Double bankslipDiscount = Double.valueOf(JSONUtils.getIntegerValueFromJSON(product, "discount_percent_boleto", 0));

      if (spotlightPrice.equals(priceFrom)) {
         priceFrom = null;
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(BankSlip.BankSlipBuilder.create().setFinalPrice(spotlightPrice).setOnPageDiscount(bankslipDiscount).build())
         .build();
   }

   private List<String> scrapSales(JSONObject product) {
      List<String> sales = new ArrayList<>();
      JSONObject pricingObject = JSONUtils.getValueRecursive(product, "pricing({\"channel\":\"fravega-ecommerce\"}).0", JSONObject.class);
      Double discount =  JSONUtils.getDoubleValueFromJSON(pricingObject, "discount", true);
      sales.add(String.valueOf(discount));
      return sales;
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


   private CategoryCollection scrapCategories(JSONObject product) {
      CategoryCollection categories = new CategoryCollection();

      JSONArray categoriesJSON = JSONUtils.getValueRecursive(product, "categorization.0", JSONArray.class);

      for (Object categoryObject : categoriesJSON) {
         JSONObject category = (JSONObject) categoryObject;
         String categoryName = category.optString("name");
         if (!categoryName.isEmpty()) {
            categories.add(categoryName);
         }
      }

      return categories;
   }

   private String scrapDescription(JSONObject product) {
      JSONArray descriptionsArray = product.optJSONArray("descriptions");
      String concatenatedValues = "";
      for (int i = 0; i < descriptionsArray.length(); i++) {
         JSONObject descriptionObject = descriptionsArray.getJSONObject(i);
         String value = descriptionObject.getString("value");
         concatenatedValues += value;
      }

      return concatenatedValues;
   }
}
