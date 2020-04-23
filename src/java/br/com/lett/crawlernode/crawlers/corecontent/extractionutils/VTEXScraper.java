
package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VtexConfig.CardsInfo;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VtexConfig.CardsInfo.CardsInfoBuilder;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
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

public abstract class VTEXScraper extends Crawler {

   public static final String SKU_ID = "sku";
   public static final String PRODUCT_ID = "productId";
   public static final String SKU_NAME = "skuname";
   public static final String PRODUCT_NAME = "name";
   public static final String PRODUCT_MODEL = "Reference";
   public static final String PRICE_FROM = "ListPrice";
   public static final String IMAGES = "Images";
   public static final String IS_PRINCIPAL_IMAGE = "IsMain";
   public static final String IMAGE_PATH = "Path";
   public static final String SELLERS_INFORMATION = "SkuSellersInformation";
   public static final String SELLER_NAME = "Name";
   public static final String SELLER_PRICE = "Price";
   public static final String SELLER_AVAILABLE_QUANTITY = "AvailableQuantity";
   public static final String IS_DEFAULT_SELLER = "IsDefaultSeller";
   public static final String BEST_INSTALLMENT_NUMBER = "BestInstallmentNumber";
   public static final String BEST_INSTALLMENT_VALUE = "BestInstallmentValue";

   public VTEXScraper(Session session) {
      super(session);
   }

   public List<Product> extractVtexInformation(Document doc, VtexConfig vtexConfig) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      JSONObject productJson = crawlSkuJsonVTEX(doc, session);
      JSONArray arraySkus = productJson != null ? productJson.optJSONArray("skus") : null;
      if (arraySkus != null) {
         String internalPid = scrapInternalPid(productJson);
         JSONArray arrayEans = scrapEans(doc);

         for (int i = 0; i < arraySkus.length(); i++) {
            JSONObject skuJson = arraySkus.getJSONObject(i);

            String internalId = scrapInternalId(skuJson);
            CategoryCollection categories = scrapCategories(doc, internalId);
            JSONObject apiJSON = crawlApi(internalId, vtexConfig.getHomePage());
            String name = scrapName(skuJson, productJson, apiJSON, doc);
            String primaryImage = scrapPrimaryImage(apiJSON);
            String secondaryImages = scrapSecondaryImages(apiJSON);
            Offers offers = scrapOffer(apiJSON, internalId, vtexConfig);
            String description = scrapDescription(doc, apiJSON, skuJson, productJson, internalId);
            String ean = i < arrayEans.length() ? arrayEans.optString(i) : null;
            List<String> eans = ean != null && ean.isEmpty() ? Arrays.asList(ean) : null;
            RatingsReviews rating = scrapRating(internalId, internalPid, doc, apiJSON);

            // Creating the product
            Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setCategory1(categories.getCategory(0))
                  .setCategory2(categories.getCategory(1))
                  .setCategory3(categories.getCategory(2))
                  .setEans(eans)
                  .setOffers(offers)
                  .setDescription(description)
                  .setRatingReviews(rating)
                  .build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   public String scrapInternalId(JSONObject json) {
      String internalId = null;

      if (json.has(SKU_ID)) {
         internalId = json.optString(SKU_ID).trim();
      }

      return internalId;
   }

   public String scrapInternalPid(JSONObject skuJson) {
      String internalPid = null;

      if (skuJson.has(PRODUCT_ID)) {
         internalPid = skuJson.optString(PRODUCT_ID).trim();
      }

      return internalPid;
   }

   protected CategoryCollection scrapCategories(Document doc, String internalId) {
      CategoryCollection categories = new CategoryCollection();

      Element category = doc.selectFirst(".bread-crumb .last a");
      if (category != null) {
         categories.add(category.ownText().trim());
      }

      return categories;
   }

   /**
    * Get JSONArray wich contains the EAN data.
    * 
    * @param doc document to be searched
    * @return JSONArray object
    */
   public JSONArray scrapEans(Document doc) {
      JSONArray arr = new JSONArray();

      JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "vtex.events.addData(", ");", true, false);
      if (json.has("productEans")) {
         arr = json.optJSONArray("productEans");
      }

      return arr == null ? new JSONArray()
            : arr;
   }

   /**
    * Fetch api data on Ex: "https://www.somesite.com/produto/sku/someid"
    * 
    * @param internalId
    * @return
    */
   public JSONObject crawlApi(String internalId, String homePage) {
      JSONObject api = new JSONObject();
      String url = homePage + "produto/sku/" + internalId;

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      JSONArray jsonArray = CrawlerUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());

      if (!jsonArray.isEmpty()) {
         api = jsonArray.optJSONObject(0);
      }

      return api == null ? new JSONObject()
            : api;
   }

   /**
    * Capture name with product model
    * 
    * @param jsonSku
    * @param skuJson
    * @param apiJson
    * @return
    */
   protected String scrapName(JSONObject jsonSku, JSONObject skuJson, JSONObject apiJson, Document doc) {
      StringBuilder name = new StringBuilder();

      String nameVariation = jsonSku.has(SKU_NAME) ? jsonSku.getString(SKU_NAME)
            : null;

      if (skuJson.has(PRODUCT_NAME)) {
         name.append(skuJson.getString(PRODUCT_NAME));

         if (nameVariation != null) {
            if (name.length() > nameVariation.length()) {
               name.append(" ").append(nameVariation);
            } else {
               name = new StringBuilder(nameVariation);
            }
         }
      }

      if (apiJson.has(PRODUCT_MODEL)) {
         name.append(" ").append(apiJson.get(PRODUCT_MODEL));
      }

      return name.toString();
   }

   public String scrapPrimaryImage(JSONObject json) {
      String primaryImage = null;

      if (json.has(IMAGES)) {
         JSONArray jsonArrayImages = json.getJSONArray(IMAGES);

         for (int i = 0; i < jsonArrayImages.length(); i++) {
            JSONArray arrayImage = jsonArrayImages.getJSONArray(i);
            JSONObject jsonImage = arrayImage.getJSONObject(0);

            if (jsonImage.has(IS_PRINCIPAL_IMAGE) && jsonImage.getBoolean(IS_PRINCIPAL_IMAGE) && jsonImage.has(IMAGE_PATH)) {
               primaryImage = changeImageSizeOnURL(jsonImage.getString(IMAGE_PATH));
               break;
            }
         }
      }

      return primaryImage;
   }

   public String scrapSecondaryImages(JSONObject apiInfo) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      if (apiInfo.has(IMAGES)) {
         JSONArray jsonArrayImages = apiInfo.getJSONArray(IMAGES);

         for (int i = 0; i < jsonArrayImages.length(); i++) {
            JSONArray arrayImage = jsonArrayImages.getJSONArray(i);
            JSONObject jsonImage = arrayImage.getJSONObject(0);

            // jump primary image
            if (jsonImage.has(IS_PRINCIPAL_IMAGE) && jsonImage.getBoolean(IS_PRINCIPAL_IMAGE)) {
               continue;
            }

            if (jsonImage.has(IMAGE_PATH)) {
               String urlImage = changeImageSizeOnURL(jsonImage.getString(IMAGE_PATH));
               secondaryImagesArray.put(urlImage);
            }

         }
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   /**
    * Get the image url and change it size
    * 
    * @param url
    * @return
    */
   public String changeImageSizeOnURL(String url) {
      String[] tokens = url.trim().split("/");
      String dimensionImage = tokens[tokens.length - 2]; // to get dimension image and the image id

      String[] tokens2 = dimensionImage.split("-"); // to get the image-id
      String dimensionImageFinal = tokens2[0] + "-1000-1000";

      return url.replace(dimensionImage, dimensionImageFinal); // The image size is changed
   }

   public Offers scrapOffer(JSONObject apiJSON, String internalId, VtexConfig vtexConfig) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      JSONArray offersArray = apiJSON.optJSONArray("SkuSellersInformation");
      if (offersArray != null) {
         int position = 1;
         for (Object o : offersArray) {
            JSONObject offerJson = o instanceof JSONObject ? (JSONObject) o
                  : new JSONObject();
            String sellerFullName = offerJson.optString("Name", null);
            String sellerId = offerJson.optString("SellerId", null);
            boolean isBuyBox = offersArray.length() > 1;
            boolean isMainRetailer = isMainRetailer(sellerFullName, vtexConfig.getMainSellerNames());

            Pricing pricing = scrapPricing(internalId, apiJSON, offerJson, vtexConfig);
            List<String> sales = scrapSales(vtexConfig, pricing, internalId);

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

      return offers;
   }

   protected List<String> scrapSales(VtexConfig vtexConfig, Pricing pricing, String internalId) {
      List<String> sales = vtexConfig.getSales();

      if (vtexConfig.isSalesIsCalculated() && pricing.getPriceFrom() != null && pricing.getPriceFrom() > pricing.getSpotlightPrice()) {
         BigDecimal big = BigDecimal.valueOf(pricing.getPriceFrom() / pricing.getSpotlightPrice() - 1);
         String rounded = big.setScale(2, BigDecimal.ROUND_DOWN).toString();
         sales.add('-' + rounded.replace("0.", "") + '%');
      }

      return sales;
   }

   private boolean isMainRetailer(String sellerName, List<String> mainSellerNames) {
      boolean isMainRetailer = false;

      for (String seller : mainSellerNames) {
         if (seller.equalsIgnoreCase(seller)) {
            isMainRetailer = true;
            break;
         }
      }

      return isMainRetailer;
   }

   private Pricing scrapPricing(String internalId, JSONObject apiJson, JSONObject sellerJson, VtexConfig vtexConfig) throws MalformedPricingException {
      boolean isDefaultSeller = sellerJson.optBoolean("IsDefaultSeller", true);

      JSONObject pricesJson = isDefaultSeller ? apiJson
            : sellerJson;
      Double spotlightPrice = pricesJson.optDouble(SELLER_PRICE, 0d);
      Double priceFrom = pricesJson.optDouble(PRICE_FROM);

      if (priceFrom <= spotlightPrice) {
         priceFrom = null;
      }

      CreditCards creditCards = vtexConfig.isUsePriceAPI() ? scrapCreditCardsFromApi(spotlightPrice, isDefaultSeller, internalId, vtexConfig)
            : scrapCreditCard(spotlightPrice, pricesJson, vtexConfig);
      BankSlip bankSlip = vtexConfig.isHasBankTicket() ? scrapBankSlip(spotlightPrice, vtexConfig.getBankDiscount()) : null;

      return PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setPriceFrom(priceFrom)
            .setCreditCards(creditCards)
            .setBankSlip(bankSlip)
            .build();
   }

   private BankSlip scrapBankSlip(Double spotlightPrice, Integer bankDiscount) throws MalformedPricingException {
      Double discount = bankDiscount != null ? bankDiscount / 100d : null;
      Double bankValue = discount != null ? MathUtils.normalizeTwoDecimalPlaces(spotlightPrice - (spotlightPrice * (discount)))
            : spotlightPrice;

      return BankSlipBuilder.create()
            .setFinalPrice(bankValue)
            .setOnPageDiscount(discount)
            .build();
   }

   private CreditCards scrapCreditCard(Double spotlightPrice, JSONObject pricesJson, VtexConfig vtexConfig) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      Double value = pricesJson.optDouble(BEST_INSTALLMENT_VALUE, 0d);
      Integer installmentNumber = pricesJson.optInt(BEST_INSTALLMENT_NUMBER);

      for (CardsInfo card : vtexConfig.getCards()) {
         installments.add(setInstallment(spotlightPrice, 1, card.getInstallmentsDiscounts()));

         if (installmentNumber > 0 && value > 0) {
            installments.add(setInstallment(value, installmentNumber, card.getInstallmentsDiscounts()));
         }

         creditCards.add(CreditCardBuilder.create()
               .setBrand(card.getBrand())
               .setInstallments(installments)
               .setIsShopCard(card.isShopCard())
               .build());
      }

      return creditCards;
   }

   private CreditCards scrapCreditCardsFromApi(Double spotlightPrice, boolean isDefaultSeller, String internalId, VtexConfig vtexConfig) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      String pricesApi = vtexConfig.getHomePage() + "productotherpaymentsystems/" + internalId;
      Request request = RequestBuilder.create().setUrl(pricesApi).setCookies(cookies).build();
      Document doc = Jsoup.parse(this.dataFetcher.get(session, request).getBody());

      if (isDefaultSeller) {
         Elements cardsElements = doc.select("#ddlCartao option");

         if (!cardsElements.isEmpty()) {
            for (Element e : cardsElements) {
               String text = e.text().toLowerCase();
               String idCard = e.val();
               String card = null;

               if (text.contains("visa")) {
                  card = Card.VISA.toString();
               } else if (text.contains("mastercard")) {
                  card = Card.MASTERCARD.toString();
               } else if (text.contains("cabal")) {
                  card = Card.CABAL.toString();
               } else if (text.contains("nativa")) {
                  card = Card.NATIVA.toString();
               } else if (text.contains("naranja")) {
                  card = Card.NARANJA.toString();
               } else if (text.contains("american express")) {
                  card = Card.AMEX.toString();
               }

               if (card != null) {
                  CardsInfo cardInfo = vtexConfig.getCardInfoByBrand(card);
                  Map<Integer, Integer> discounts = cardInfo != null ? cardInfo.getInstallmentsDiscounts()
                        : null;
                  Installments installments = scrapInstallments(doc, idCard, spotlightPrice, discounts);
                  creditCards.add(CreditCardBuilder.create()
                        .setBrand(card)
                        .setInstallments(installments)
                        .setIsShopCard(false)
                        .build());
               }
            }
         }
      }

      if (creditCards.getCreditCards().isEmpty()) {
         for (CardsInfo card : vtexConfig.getCards()) {
            Installments installments = new Installments();
            installments.add(setInstallment(spotlightPrice, 1, card.getInstallmentsDiscounts()));

            creditCards.add(CreditCardBuilder.create()
                  .setBrand(card.getBrand())
                  .setInstallments(installments)
                  .setIsShopCard(card.isShopCard())
                  .build());
         }
      }


      return creditCards;
   }


   public Installments scrapInstallments(Document doc, String idCard, Double spotlightPrice, Map<Integer, Integer> discounts) throws MalformedPricingException {
      Installments installments = new Installments();

      Elements installmentsCard = doc.select(".tbl-payment-system#tbl" + idCard + " tr");
      for (Element i : installmentsCard) {
         Element installmentElement = i.select("td.parcelas").first();

         if (installmentElement != null) {
            String textInstallment = installmentElement.text().toLowerCase();
            Integer installment = null;

            if (textInstallment.contains("vista")) {
               installment = 1;
            } else {
               String text = textInstallment.replaceAll("[^0-9]", "").trim();

               if (!text.isEmpty()) {
                  installment = Integer.parseInt(text);
               }
            }

            Element valueElement = i.select("td:not(.parcelas)").first();

            if (valueElement != null && installment != null) {
               Double value = MathUtils.parseDoubleWithComma(valueElement.text());

               installments.add(setInstallment(value, installment, discounts));
            }
         }
      }

      if (installments.getInstallment(1) == null) {
         installments.add(setInstallment(spotlightPrice, 1, discounts));
      }

      return installments;
   }

   private Installment setInstallment(Double value, int installmentNumber, Map<Integer, Integer> discounts) throws MalformedPricingException {
      Double installmentValue = value;

      if (discounts != null && discounts.containsKey(installmentNumber)) {
         installmentValue = MathUtils.normalizeTwoDecimalPlaces(value - (value * (discounts.get(installmentNumber) / 100d)));
      }

      return InstallmentBuilder.create()
            .setInstallmentNumber(installmentNumber)
            .setInstallmentPrice(installmentValue)
            .build();
   }

   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject apiJson) {
      return null;
   }

   /**
    * @param document
    * @param apiJSON
    * @return
    */
   protected String scrapDescription(Document document, JSONObject apiJSON, JSONObject skuJson, JSONObject productJson, String internalId) {
      return new StringBuilder().toString();
   }

   /**
    * Crawl description api on this url examples:
    * https://www.thebeautybox.com.br/api/catalog_system/pub/products/search?fq=skuId:19245
    * https://www.drogariasaopaulo.com.br/api/catalog_system/pub/products/search?fq=productId:19245
    * 
    * HOME_PAGE + api/catalog_system/pub/products/search?fq= + idType(sku or product) + : + id
    * 
    * @param id
    * @param idType
    * @return
    */
   public JSONObject crawlCatalogAPI(String id, String idType, String homePage) {
      JSONObject json = new JSONObject();

      String url = homePage + "api/catalog_system/pub/products/search?fq=" + idType + ":" + id;

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      JSONArray array = CrawlerUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());

      if (array.length() > 0) {
         json = array.getJSONObject(0);
      }

      return json;
   }

   public Document sanitizeDescription(Object obj) {
      return Jsoup.parse(obj.toString().replace("[\"", "").replace("\"]", "").replace("\\r\\n\\r\\n\\r\\n", "").replace("\\", ""));
   }

   protected List<CardsInfo> setListOfCards(Set<String> cards, Map<Integer, Integer> discounts) {
      List<CardsInfo> cardsInfo = new ArrayList<>();

      for (String card : cards) {
         cardsInfo.add(CardsInfoBuilder.create()
               .setCard(card)
               .setShopCard(false)
               .setInstallmentsDiscounts(discounts)
               .build());
      }

      return cardsInfo;
   }

   /**
    * Crawl skuJson from html in VTEX Sites
    * 
    * @param document
    * @param session
    * @return
    */
   public static JSONObject crawlSkuJsonVTEX(Document document, Session session) {
      Elements scriptTags = document.getElementsByTag("script");
      String scriptVariableName = "var skuJson_0 = ";
      JSONObject skuJson = new JSONObject();
      String skuJsonString = null;

      for (Element tag : scriptTags) {
         for (DataNode node : tag.dataNodes()) {
            if (tag.html().trim().startsWith(scriptVariableName)) {
               skuJsonString = node.getWholeData().split(Pattern.quote(scriptVariableName))[1]
                     + node.getWholeData().split(Pattern.quote(scriptVariableName))[1].split(Pattern.quote("};"))[0];
               break;
            }
         }
      }

      if (skuJsonString != null) {
         try {
            skuJson = new JSONObject(skuJsonString);

         } catch (JSONException e) {
            Logging.printLogWarn(logger, session, "Error creating JSONObject from var skuJson_0");
            Logging.printLogWarn(logger, session, CommonMethods.getStackTraceString(e));
         }
      }

      return skuJson;
   }
}
