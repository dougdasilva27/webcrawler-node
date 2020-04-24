package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

public abstract class VTEXNewScraper extends Crawler {

   public VTEXNewScraper(Session session) {
      super(session);
   }

   private static final String CARD_REGEX = "(?i)^[a-zÀ-ú]+[ ]?(?!a )(?!á )(?!à )[a-zÀ-ú]+";

   private final String homePage = getHomePage();
   private final List<String> mainSellersNames = getMainSellersNames();

   protected abstract String getHomePage();

   protected abstract List<String> getMainSellersNames();

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(homePage));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      JSONObject runTimeJSON = scrapRuntimeJson(doc);
      JSONObject productJson = scrapProductJson(runTimeJSON);

      if (productJson.has("productId")) {
         String internalPid = productJson.has("productId") && !productJson.isNull("productId") ? productJson.get("productId").toString() : null;
         CategoryCollection categories = scrapCategories(productJson);
         String description = JSONUtils.getStringValue(productJson, "description");

         JSONArray items = productJson.has("items") && !productJson.isNull("items") ? productJson.getJSONArray("items") : new JSONArray();

         for (int i = 0; i < items.length(); i++) {
            JSONObject jsonSku = items.getJSONObject(i);

            Product product = extractProduct(doc, internalPid, categories, description, jsonSku);
            products.add(product);
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
      }

      return products;
   }

   protected Product extractProduct(Document doc, String internalPid, CategoryCollection categories, String description, JSONObject jsonSku) throws Exception {

      String internalId = jsonSku.has("itemId") ? jsonSku.get("itemId").toString() : null;
      String name = jsonSku.has("nameComplete") ? jsonSku.get("nameComplete").toString() : null;
      List<String> images = scrapImages(jsonSku);
      String primaryImage = !images.isEmpty() ? images.get(0) : null;
      String secondaryImages = scrapSecondaryImages(images);
      Offers offers = scrapOffer(doc, jsonSku, internalId, internalPid);
      RatingsReviews rating = scrapRating(internalId, internalPid, doc, jsonSku);
      List<String> eans = jsonSku.has("ean") ? Arrays.asList(jsonSku.get("ean").toString()) : null;

      // Creating the product
      return ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setOffers(offers)
            .setDescription(description)
            .setEans(eans)
            .setRatingReviews(rating)
            .build();
   }

   private JSONObject scrapRuntimeJson(Document doc) {
      JSONObject runtimeJson = new JSONObject();
      String token = "__RUNTIME__ =";

      Elements scripts = doc.select("script");
      for (Element e : scripts) {
         String script = e.html();

         if (script.contains(token)) {
            runtimeJson = CrawlerUtils.stringToJSONObject(CrawlerUtils.extractSpecificStringFromScript(script, token, false, "", true));
            break;
         }
      }

      return runtimeJson;
   }

   private JSONObject scrapProductJson(JSONObject stateJson) {
      JSONObject product = new JSONObject();
      Object queryData = JSONUtils.getValue(stateJson, "queryData");
      JSONObject queryDataJson = new JSONObject();

      if (queryData instanceof JSONObject) {
         queryDataJson = (JSONObject) queryData;
      } else if (queryData instanceof JSONArray) {
         JSONArray queryDataArray = (JSONArray) queryData;
         if (queryDataArray.length() > 0 && queryDataArray.get(0) instanceof JSONObject) {
            queryDataJson = queryDataArray.getJSONObject(0);
         }
      }

      if (queryDataJson.has("data") && queryDataJson.get("data") instanceof JSONObject) {
         product = queryDataJson.getJSONObject("data");
      } else if (queryDataJson.has("data") && queryDataJson.get("data") instanceof String) {
         product = CrawlerUtils.stringToJson(queryDataJson.getString("data"));
      }

      return JSONUtils.getJSONValue(product, "product");
   }

   private CategoryCollection scrapCategories(JSONObject product) {
      CategoryCollection categories = new CategoryCollection();

      JSONArray categoriesArray = JSONUtils.getJSONArrayValue(product, "categories");
      for (int i = categoriesArray.length() - 1; i >= 0; i--) {
         String path = categoriesArray.get(i).toString();

         if (path.contains("/")) {
            categories.add(CommonMethods.getLast(path.split("/")));
         }
      }

      return categories;
   }

   private List<String> scrapImages(JSONObject skuJson) {
      List<String> images = new ArrayList<>();

      for (String key : skuJson.keySet()) {
         if (key.startsWith("images")) {
            JSONArray imagesArray = skuJson.getJSONArray(key);

            for (Object o : imagesArray) {
               JSONObject image = (JSONObject) o;

               if (image.has("imageUrl") && !image.isNull("imageUrl")) {
                  images.add(CrawlerUtils.completeUrl(image.get("imageUrl").toString(), "https", "jumbo.vteximg.com.br"));
               }
            }

            break;
         }
      }

      return images;
   }

   private String scrapSecondaryImages(List<String> images) {
      String secondaryImages = null;
      JSONArray imagesArray = new JSONArray();

      if (!images.isEmpty()) {
         images.remove(0);

         for (String image : images) {
            imagesArray.put(image);
         }
      }

      if (imagesArray.length() > 0) {
         secondaryImages = imagesArray.toString();
      }

      return secondaryImages;
   }

   public Offers scrapOffer(Document doc, JSONObject jsonSku, String internalId, String internalPid) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      JSONArray sellers = jsonSku.getJSONArray("sellers");
      if (sellers != null) {
         int position = 1;
         for (Object o : sellers) {
            JSONObject offerJson = o instanceof JSONObject ? (JSONObject) o
                  : new JSONObject();
            JSONObject commertialOffer = offerJson.optJSONObject("commertialOffer");
            String sellerFullName = offerJson.optString("sellerName", null);

            if (commertialOffer != null && sellerFullName != null) {
               String sellerId = offerJson.optString("sellerId", null);
               boolean isBuyBox = sellers.length() > 1;
               boolean isMainRetailer = isMainRetailer(sellerFullName, mainSellersNames);

               Pricing pricing = scrapPricing(commertialOffer);
               List<String> sales = scrapSales(doc, offerJson, internalId, internalPid);

               offers.add(OfferBuilder.create()
                     .setInternalSellerId(sellerId)
                     .setSellerFullName(sellerFullName)
                     .setMainPagePosition(position)
                     .setIsBuybox(isBuyBox)
                     .setIsMainRetailer(isMainRetailer)
                     .setPricing(pricing)
                     .setSales(sales)
                     .build());

               position++;
            }
         }
      }

      return offers;
   }

   protected abstract List<String> scrapSales(Document doc, JSONObject offerJson, String internalId, String internalPid);

   private boolean isMainRetailer(String sellerName, List<String> mainSellerNames) {
      boolean isMainRetailer = false;

      for (String seller : mainSellerNames) {
         if (seller.startsWith(sellerName)) {
            isMainRetailer = true;
            break;
         }
      }

      return isMainRetailer;
   }

   private Pricing scrapPricing(JSONObject comertial) throws MalformedPricingException {
      Double spotlightPrice = comertial.optDouble("spotPrice");
      Double priceFrom = comertial.optDouble("ListPrice");

      if (priceFrom != null && spotlightPrice != null && spotlightPrice == priceFrom) {
         priceFrom = null;
      }

      BankSlip bankSlip = scrapBankSlip(spotlightPrice, comertial);
      CreditCards creditCards = scrapCreditCards(comertial);

      return PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setPriceFrom(priceFrom)
            .setBankSlip(bankSlip)
            .setCreditCards(creditCards)
            .build();
   }

   private CreditCards scrapCreditCards(JSONObject comertial) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      JSONArray installmentsArray = comertial.optJSONArray("Installments");
      if (installmentsArray != null) {
         Map<String, Installments> cardsInstallments = new HashMap<>();

         for (Object o : installmentsArray) {
            JSONObject installmentJson = (JSONObject) o;

            String cardName = extractCardName(installmentJson.optString("name"));
            Integer installmentNumber = installmentJson.optInt("NumberOfInstallments");
            Double totalValue = installmentJson.optDouble("TotalValuePlusInterestRate");
            Double value = installmentJson.optDouble("Value");
            Double interest = installmentJson.optDouble("InterestRate");

            Installment installment = InstallmentBuilder.create()
                  .setInstallmentNumber(installmentNumber)
                  .setInstallmentPrice(value)
                  .setAmOnPageInterests(interest)
                  .setFinalPrice(totalValue)
                  .build();

            if (cardsInstallments.containsKey(cardName)) {
               Installments installments = cardsInstallments.get(cardName);
               installments.add(installment);

               cardsInstallments.put(cardName, installments);
            } else {
               Installments installments = new Installments();
               installments.add(installment);

               cardsInstallments.put(cardName, installments);
            }
         }

         for (Entry<String, Installments> entry : cardsInstallments.entrySet()) {
            creditCards.add(CreditCardBuilder.create()
                  .setBrand(entry.getKey())
                  .setInstallments(entry.getValue())
                  .setIsShopCard(false)
                  .build());
         }
      }

      return creditCards;
   }

   private String extractCardName(String input) {
      String cardName = null;
      Matcher opa = Pattern.compile(CARD_REGEX).matcher(input);
      while (opa.find() && cardName == null) {
         cardName = opa.group();
      }

      return cardName;
   }

   private BankSlip scrapBankSlip(Double spotlightPrice, JSONObject comertial) throws MalformedPricingException {
      Double bankSlipPrice = spotlightPrice;

      JSONArray installmentsArray = comertial.optJSONArray("Installments");
      if (installmentsArray != null) {
         for (Object o : installmentsArray) {
            JSONObject installmentJson = (JSONObject) o;

            String name = installmentJson.optString("name");
            if (name.toLowerCase().contains("boleto")) {
               bankSlipPrice = installmentJson.optDouble("Value");
               break;
            }
         }
      }

      return BankSlipBuilder.create()
            .setFinalPrice(bankSlipPrice)
            .build();
   }

   protected abstract RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku);
}
