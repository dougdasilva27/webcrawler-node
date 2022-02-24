package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.*;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Pricing.PricingBuilder;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.UnsupportedEncodingException;
import java.util.*;

public abstract class VTEXScraper extends Crawler {

   public VTEXScraper(Session session) {
      super(session);
   }


   protected String storeCard = null;
   protected final String homePage = getHomePage();
   protected final List<String> mainSellersNames = getMainSellersNames();

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

      String internalPid = scrapPidFromApi(doc);

      if (internalPid != null && isProductPage(doc)) {
         JSONObject productJson = crawlProductApi(internalPid, null);

         CategoryCollection categories = scrapCategories(productJson);
         String description = scrapDescription(doc, productJson);
         processBeforeScrapVariations(doc, productJson, internalPid);
         if (productJson != null) {
            JSONArray items = JSONUtils.getJSONArrayValue(productJson, "items");

            if(items.length()>0) {
               for (int i = 0; i < items.length(); i++) {
                  JSONObject jsonSku = items.optJSONObject(i);
                  if (jsonSku == null) {
                     jsonSku = new JSONObject();
                  }
                  Product product = extractProduct(doc, internalPid, categories, description, jsonSku, productJson);
                  products.add(product);
               }
            }else {
               Product product = extractProductHtml(doc,internalPid);
               if (product != null){
                  products.add(product);
               }
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
      }

      return products;
   }

   protected Product extractProductHtml(Document doc, String internalPid) throws MalformedProductException, OfferException, MalformedPricingException {
      return null;
   }

   protected void processBeforeScrapVariations(Document doc, JSONObject productJson, String internalPid) throws UnsupportedEncodingException {
   }

   protected boolean isProductPage(Document doc) {
      return true;
   }

   protected Product extractProduct(Document doc, String pidFromApi, CategoryCollection categories, String description, JSONObject jsonSku, JSONObject productJson) throws Exception {
      String internalId = jsonSku.optString("itemId", null);
      String name = scrapName(doc, productJson, jsonSku);
      List<String> images = scrapImages(doc, jsonSku, pidFromApi, internalId);
      String primaryImage = !images.isEmpty() ? images.get(0) : null;
      scrapSecondaryImages(images); // remove first position
      Offers offers = scrapOffer(doc, jsonSku, internalId, pidFromApi);
      RatingsReviews rating = scrapRating(internalId, pidFromApi, doc, jsonSku);
      List<String> eans = jsonSku.has("ean") ? Arrays.asList(jsonSku.get("ean").toString()) : null;
      String internalPid = scrapInternalPid(doc, productJson, pidFromApi);

      // Creating the product
      return ProductBuilder.create()
         .setUrl(session.getOriginalURL())
         .setInternalId(internalId)
         .setInternalPid(internalPid)
         .setName(name)
         .setCategories(categories)
         .setPrimaryImage(primaryImage)
         .setSecondaryImages(images)
         .setOffers(offers)
         .setDescription(description)
         .setEans(eans)
         .setRatingReviews(rating)
         .build();
   }

   public String scrapInternalPid(Document doc, JSONObject jsonObject, String pidFromApi) {
      return pidFromApi;
   }

   protected abstract String scrapPidFromApi(Document doc);

   protected String scrapName(Document doc, JSONObject productJson, JSONObject jsonSku) {
      String name = null;
      if (jsonSku.has("nameComplete")) {
         name = jsonSku.optString("nameComplete");
      } else if (jsonSku.has("name")) {
         name = jsonSku.optString("name");
      }

      if (name != null && !name.isEmpty() && productJson.has("brand")) {
         String brand = productJson.optString("brand");
         if (brand != null && !brand.isEmpty() && !checkIfNameHasBrand(brand, name)){
            name = name + " " + brand;
         }
      }

      return name;
   }

   private boolean checkIfNameHasBrand(String brand, String name) {
         String brandStripAccents = StringUtils.stripAccents(brand);
         String nameStripAccents = StringUtils.stripAccents(name);
         return nameStripAccents.toLowerCase(Locale.ROOT).contains(brandStripAccents.toLowerCase(Locale.ROOT));
   }

   protected CategoryCollection scrapCategories(JSONObject product) {
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

   protected String scrapDescription(Document doc, JSONObject productJson) throws UnsupportedEncodingException {
      return JSONUtils.getStringValue(productJson, "description");
   }

   protected Document sanitizeDescription(Object obj) {
      return Jsoup.parse(obj.toString().replace("[\"", "").replace("\"]", "").replace("\\r\\n\\r\\n\\r\\n", "").replace("\\", ""));
   }

   protected String scrapSpecsDescriptions(JSONObject productJson) {
      StringBuilder description = new StringBuilder();

      List<String> specs = new ArrayList<>();

      if (productJson != null && productJson.has("allSpecifications")) {
         JSONArray keys = productJson.getJSONArray("allSpecifications");
         for (Object o : keys) {
            if (!o.toString().equalsIgnoreCase("Informações para Instalação") && !o.toString().equalsIgnoreCase("Portfólio")) {
               specs.add(o.toString());
            }
         }
      }

      for (String spec : specs) {

         description.append("<div>");
         description.append("<h4>").append(spec).append("</h4>");
         description.append(sanitizeDescription(productJson.get(spec)));
         description.append("</div>");
      }

      return description.toString();
   }

   protected List<String> scrapImages(Document doc, JSONObject skuJson, String internalPid, String internalId) {
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

   protected void scrapSecondaryImages(List<String> images) {
      if (!images.isEmpty()) {
         images.remove(0);
      }
   }

   protected Offers scrapOffer(Document doc, JSONObject jsonSku, String internalId, String internalPid) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      JSONArray sellers = jsonSku.optJSONArray("sellers");
      if (sellers != null) {
         int position = 1;
         for (Object o : sellers) {
            JSONObject offerJson = o instanceof JSONObject ? (JSONObject) o
               : new JSONObject();
            JSONObject commertialOffer = offerJson.optJSONObject("commertialOffer");
            String sellerFullName = offerJson.optString("sellerName", null);
            boolean isDefaultSeller = offerJson.optBoolean("sellerDefault", true);

            if (commertialOffer != null && sellerFullName != null) {
               Integer stock = commertialOffer.optInt("AvailableQuantity");

               if (stock > 0) {
                  JSONObject discounts = scrapDiscounts(commertialOffer);

                  String sellerId = offerJson.optString("sellerId", null);
                  boolean isBuyBox = sellers.length() > 1;
                  boolean isMainRetailer = isMainRetailer(sellerFullName);

                  Pricing pricing = scrapPricing(doc, internalId, commertialOffer, discounts);
                  List<String> sales = isDefaultSeller ? scrapSales(doc, offerJson, internalId, internalPid, pricing) : new ArrayList<>();

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
      }

      return offers;
   }

   protected List<String> scrapSales(Document doc, JSONObject offerJson, String internalId, String internalPid, Pricing pricing) {
      List<String> sales = new ArrayList<>();
      if(pricing != null) sales.add(CrawlerUtils.calculateSales(pricing));
      return sales;
   }

   protected boolean isMainRetailer(String sellerName) {
      return mainSellersNames.stream().anyMatch(seller -> seller.toLowerCase().startsWith(sellerName.toLowerCase()));
   }

   protected Pricing scrapPricing(Document doc, String internalId, JSONObject comertial, JSONObject discountsJson) throws MalformedPricingException {
      Double principalPrice = comertial.optDouble("Price");
      Double priceFrom = comertial.optDouble("ListPrice");

      CreditCards creditCards = scrapCreditCards(comertial, discountsJson, true);
      BankSlip bankSlip = scrapBankSlip(principalPrice, comertial, discountsJson, true);

      Double spotlightPrice = scrapSpotlightPrice(doc, internalId, principalPrice, comertial, discountsJson);
      if (priceFrom != null && spotlightPrice != null && spotlightPrice.equals(priceFrom)) {
         priceFrom = null;
      }

      return PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   protected Double scrapSpotlightPrice(Document doc, String internalId, Double principalPrice, JSONObject comertial, JSONObject discountsJson) {
      Double spotlightPrice = principalPrice;
      Double maxDiscount = 0d;
      if (discountsJson != null && discountsJson.length() > 0) {
         for (String key : discountsJson.keySet()) {
            JSONObject paymentEffect = discountsJson.optJSONObject(key);
            Double discount = paymentEffect.optDouble("discount");

            if (discount > maxDiscount) {
               maxDiscount = discount;
            }
         }
      }

      if (maxDiscount > 0d) {
         spotlightPrice = MathUtils.normalizeTwoDecimalPlaces(spotlightPrice - (spotlightPrice * maxDiscount));
      }

      return spotlightPrice;
   }

   /**
    * This function return a Object that have the disocunt informatiosn for each payment method
    * <p>
    * <p>
    * { "1":{ "minInstallment":1, "maxInstallment":3, "discount":0.5 }, "2":{ "minInstallment":1,
    * "maxInstallment":3, "discount":0.5 } }
    * <p>
    * When "1" and "2" are the payment codes
    *
    * @param comertial
    * @return
    */
   protected JSONObject scrapDiscounts(JSONObject comertial) {
      JSONObject discountsJSON = new JSONObject();

      JSONArray teasers = comertial.optJSONArray("Teasers");
      if (teasers != null && !teasers.isEmpty()) {
         for (Object o : teasers) {
            JSONObject teaser = (JSONObject) o;

            Double discount = scrapPaymentDiscount(teaser);
            JSONObject condition = teaser.optJSONObject("<Conditions>k__BackingField");
            JSONArray paymentConditions = condition != null ? condition.optJSONArray("<Parameters>k__BackingField") : null;

            if (paymentConditions != null) {
               List<String> paymentMethodsWithConditions = new ArrayList<>();
               Integer minInstallment = 0;
               Integer maxInstallment = 0;
               for (Object obj : paymentConditions) {
                  JSONObject paymentCondition = (JSONObject) obj;

                  String conditionName = paymentCondition.optString("<Name>k__BackingField");

                  if (conditionName.equalsIgnoreCase("PaymentMethodId")) {
                     paymentMethodsWithConditions = Arrays.asList(paymentCondition.optString("<Value>k__BackingField").split(","));
                  } else if (conditionName.equalsIgnoreCase("MinInstallmentCount")) {
                     String installment = paymentCondition.optString("<Value>k__BackingField").replaceAll("[^0-9]", "");

                     if (!installment.isEmpty()) {
                        minInstallment = Integer.parseInt(installment);
                     }
                  } else if (conditionName.equalsIgnoreCase("MaxInstallmentCount")) {
                     String installment = paymentCondition.optString("<Value>k__BackingField").replaceAll("[^0-9]", "");

                     if (!installment.isEmpty()) {
                        maxInstallment = Integer.parseInt(installment);
                     }
                  }
               }

               JSONObject value = new JSONObject()
                  .put("minInstallment", minInstallment)
                  .put("maxInstallment", maxInstallment)
                  .put("discount", discount);

               for (String paymentMethodId : paymentMethodsWithConditions) {
                  discountsJSON.put(paymentMethodId, value);
               }
            }
         }
      }

      return discountsJSON;
   }

   protected Double scrapPaymentDiscount(JSONObject teaser) {
      Double discount = 0d;

      JSONObject effects = teaser.optJSONObject("<Effects>k__BackingField");
      JSONArray paymentEffects = effects != null ? effects.optJSONArray("<Parameters>k__BackingField") : null;
      if (paymentEffects != null) {
         for (Object obj : paymentEffects) {
            JSONObject paymentEffect = (JSONObject) obj;
            String effectName = paymentEffect.optString("<Name>k__BackingField");
            if (effectName.toLowerCase().contains("discount")) {
               Double value = JSONUtils.getDoubleValueFromJSON(paymentEffect, "<Value>k__BackingField", true);

               if (value != null) {
                  discount = value / 100d;
               }

               break;
            }
         }
      }

      return discount;
   }

   protected CreditCards scrapCreditCards(JSONObject comertial, JSONObject discounts, boolean mustSetDiscount) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      JSONObject paymentOptions = comertial.optJSONObject("PaymentOptions");
      if (paymentOptions != null) {
         JSONArray cardsArray = paymentOptions.optJSONArray("installmentOptions");
         if (cardsArray != null) {
            for (Object o : cardsArray) {
               JSONObject cardJson = (JSONObject) o;

               String paymentCode = cardJson.optString("paymentSystem");
               JSONObject cardDiscount = discounts.has(paymentCode) ? discounts.optJSONObject(paymentCode) : null;
               String paymentName = cardJson.optString("paymentName");
               JSONArray installmentsArray = cardJson.optJSONArray("installments");

               if (!installmentsArray.isEmpty() && !paymentName.toLowerCase().contains("boleto")) {
                  Installments installments = new Installments();
                  for (Object object : installmentsArray) {
                     JSONObject installmentJson = (JSONObject) object;

                     Integer installmentNumber = installmentJson.optInt("count");
                     Double discount = 0d;
                     Double totalValue = installmentJson.optDouble("total") / 100d;
                     Double value = installmentJson.optDouble("value") / 100d;
                     Double interest = installmentJson.optDouble("interestRate");

                     if (cardDiscount != null) {
                        int minInstallment = cardDiscount.optInt("minInstallment");
                        int maxInstallment = cardDiscount.optInt("maxInstallment");

                        if (installmentNumber >= minInstallment && installmentNumber <= maxInstallment) {
                           discount = cardDiscount.optDouble("discount");
                           value = MathUtils.normalizeTwoDecimalPlaces(value - (value * discount));
                           totalValue = MathUtils.normalizeTwoDecimalPlaces(installmentNumber * value);
                        }
                     }

                     installments.add(setInstallment(installmentNumber, value, interest, totalValue, mustSetDiscount ? discount : null));
                  }

                  String cardBrand = null;
                  for (Card card : Card.values()) {
                     if (card.toString().toLowerCase().contains(paymentName.toLowerCase())) {
                        cardBrand = card.toString();
                        break;
                     }
                  }

                  boolean isShopCard = false;
                  if (cardBrand == null) {
                     for (String sellerName : mainSellersNames) {
                        if ((storeCard != null && paymentName.equalsIgnoreCase(storeCard)) ||
                           paymentName.toLowerCase().contains(sellerName.toLowerCase())) {
                           isShopCard = true;
                           cardBrand = paymentName;
                           break;
                        }
                     }
                  }

                  if (cardBrand != null) {
                     creditCards.add(CreditCardBuilder.create()
                        .setBrand(cardBrand)
                        .setInstallments(installments)
                        .setIsShopCard(isShopCard)
                        .build());
                  }
               }
            }
         }
      }

      return creditCards;
   }

   protected Installment setInstallment(Integer installmentNumber, Double value, Double interests, Double totalValue, Double discount) throws MalformedPricingException {
      if (interests != null && (interests.isNaN() || interests.isInfinite())) {
         interests = null;
      }

      return InstallmentBuilder.create()
         .setInstallmentNumber(installmentNumber)
         .setInstallmentPrice(value)
         .setAmOnPageInterests(interests)
         .setFinalPrice(totalValue)
         .setOnPageDiscount(discount)
         .build();
   }

   protected BankSlip scrapBankSlip(Double spotlightPrice, JSONObject comertial, JSONObject discounts, boolean mustSetDiscount) throws MalformedPricingException {
      Double bankSlipPrice = spotlightPrice;
      Double discount = 0d;

      JSONObject paymentOptions = comertial.optJSONObject("PaymentOptions");
      if (paymentOptions != null) {
         JSONArray cardsArray = paymentOptions.optJSONArray("installmentOptions");
         if (cardsArray != null) {
            for (Object o : cardsArray) {
               JSONObject paymentJson = (JSONObject) o;

               String paymentCode = paymentJson.optString("paymentSystem");
               JSONObject paymentDiscount = discounts.has(paymentCode) ? discounts.optJSONObject(paymentCode) : null;
               String name = paymentJson.optString("paymentName");

               if (name.toLowerCase().contains("boleto")) {
                  bankSlipPrice = paymentJson.optDouble("value") / 100;
                  if (paymentDiscount != null) {
                     discount = paymentDiscount.optDouble("discount");
                     bankSlipPrice = MathUtils.normalizeTwoDecimalPlaces(bankSlipPrice - (bankSlipPrice * discount));
                  }

                  break;
               }
            }
         }
      }

      if (!mustSetDiscount) {
         discount = null;
      }

      return BankSlipBuilder.create()
         .setFinalPrice(bankSlipPrice)
         .setOnPageDiscount(discount)
         .build();
   }

   protected abstract RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku);

   protected JSONObject crawlProductApi(String internalPid, String parameters) {
      JSONObject productApi = new JSONObject();

      String url = homePage + "api/catalog_system/pub/products/search?fq=productId:" + internalPid + (parameters == null ? "" : parameters);

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      JSONArray array = CrawlerUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());

      if (!array.isEmpty()) {
         productApi = array.optJSONObject(0) == null ? new JSONObject() : array.optJSONObject(0);
      }

      return productApi;
   }


}
