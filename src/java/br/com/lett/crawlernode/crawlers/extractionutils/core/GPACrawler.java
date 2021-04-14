package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.*;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.prices.Prices;
import models.pricing.BankSlip;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class GPACrawler extends Crawler {

   protected String homePageHttps;
   protected String storeId;
   protected String store;
   protected String cep;
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   private static final String END_POINT_REQUEST = "https://api.gpa.digital/";

   private String MAIN_SELLER_NAME;

   public GPACrawler(Session session) {
      super(session);
      inferFields();
      super.config.setFetcher(FetchMode.FETCHER);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      if (this.cep != null) {
         fetchStoreId();
         BasicClientCookie cookie = new BasicClientCookie("ep.selected_store", this.storeId);
         cookie.setDomain(
            homePageHttps.substring(homePageHttps.indexOf("www"), homePageHttps.length() - 1));
         cookie.setPath("/");
         this.cookies.add(cookie);
      }
   }

   /**
    * Given a CEP it send a request to an API then returns the id used by GPA.
    */
   private void fetchStoreId() {

      String url = END_POINT_REQUEST + this.store + "/delivery/options?zipCode=" + this.cep.replace("-", "");

      Request request =
         RequestBuilder.create()
            .setUrl(url)
            .setCookies(cookies)
            .build();

      String response = this.dataFetcher.get(session, request).getBody();
      JSONObject jsonObjectGPA = JSONUtils.stringToJson(response);
      if (jsonObjectGPA.optJSONObject("content") instanceof JSONObject) {
         JSONObject jsonObject = jsonObjectGPA.optJSONObject("content");
         if (jsonObject.optJSONArray("deliveryTypes") instanceof JSONArray) {
            JSONArray jsonDeliveryTypes = jsonObject.optJSONArray("deliveryTypes");

            if (jsonDeliveryTypes.optJSONObject(0) instanceof JSONObject) {
               this.storeId = jsonDeliveryTypes.optJSONObject(0).optString("storeid");
            }
            for (Object object : jsonDeliveryTypes) {
               JSONObject deliveryType = (JSONObject) object;
               if (deliveryType.optString("name") instanceof String
                  && deliveryType.optString("name").contains("TRADICIONAL")) {
                  this.storeId = deliveryType.optString("storeid");
                  break;
               }
            }
         }
      }
   }

   /**
    * Infers classes' fields by reflection
    */
   private void inferFields() {
      String className = this.getClass().getSimpleName().toLowerCase();
      if (className.contains("paodeacucar")) {
         this.store = "pa";
         this.homePageHttps = "https://www.paodeacucar.com/";
         MAIN_SELLER_NAME = "Pão de Açúcar";
      } else if (className.contains("extra")) {
         this.store = "ex";
         this.homePageHttps = "https://www.clubeextra.com.br/";
         MAIN_SELLER_NAME = "clube extra";
      }
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      String productUrl = session.getOriginalURL();
      JSONObject jsonSku = crawlProductInformatioFromGPAApi(productUrl);

      if (jsonSku.has("id")) {
         Logging.printLogDebug(
            logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject data = JSONUtils.getValueRecursive(jsonSku, "sellInfos.0", JSONObject.class);

         String internalId = crawlInternalId(jsonSku);
         String internalPid = crawlInternalPid(jsonSku);
         CategoryCollection categories = crawlCategories(jsonSku);
         String description = crawlDescription(jsonSku, internalId);
         boolean available = data != null && crawlAvailability(data);
         boolean hasMarketPlace = hasMarketPlace(doc);
         Offers offers = new Offers();

         if (available) {
            offers = hasMarketPlace ? offersFromMarketPlace(doc):scrapOffers(data) ;
         }
         String primaryImage = crawlPrimaryImage(jsonSku);
         String name = crawlName(jsonSku);
         RatingsReviews ratingsReviews = extractRatingAndReviews(internalId);
         List<String> secondaryImages = crawlSecondaryImages(jsonSku, primaryImage);

         String redirectedToURL = session.getRedirectedToURL(productUrl);
         if (internalPid != null && redirectedToURL != null && !redirectedToURL.isEmpty()) {
            productUrl = redirectedToURL;
         }

         Product product =
            ProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setOffers(offers)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setRatingReviews(ratingsReviews)
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private String crawlInternalId(JSONObject json) {
      String internalId = null;

      if (json.has("id")) {
         internalId = json.get("id").toString();
      }

      return internalId;
   }

   private String crawlInternalPid(JSONObject json) {
      String internalPid = null;

      if (json.has("id")) {
         internalPid = json.getString("sku");
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

   private Double crawlPriceFrom(JSONObject json) {
      Double price = null;

      if (json.has("priceFrom")) {
         Object pObj = json.get("priceFrom");

         if (pObj instanceof Double) {
            price = (Double) pObj;
         }
      }

      return price;
   }

   private Float crawlPrice(JSONObject json) {
      Float price = null;

      if (json.has("currentPrice")) {
         Object pObj = json.get("currentPrice");

         if (pObj instanceof Double) {
            price = MathUtils.normalizeTwoDecimalPlaces(((Double) pObj).floatValue());
         }
      }

      if (json.has("productPromotion")) {
         JSONObject productPromotion = json.getJSONObject("productPromotion");

         if (productPromotion.has("unitPrice") && productPromotion.has("promotionPercentOffOnUnity")) {
            Object promotionPercentOffOnUnity = productPromotion.get("promotionPercentOffOnUnity");

            if (promotionPercentOffOnUnity instanceof Integer) {
               Integer promotion = (Integer) promotionPercentOffOnUnity;

               if (promotion == 1) {
                  price = CrawlerUtils.getFloatValueFromJSON(productPromotion, "unitPrice");
               }
            }
         }
      }

      return price;
   }

   private boolean crawlAvailability(JSONObject data) {
      return data.has("stock") && data.getBoolean("stock");
   }

   private String crawlPrimaryImage(JSONObject json) {
      String primaryImage = null;

      if (json.has("mapOfImages")) {
         JSONObject images = json.getJSONObject("mapOfImages");

         for (int i = 0; i < images.length(); i++) {
            if (images.length() > 0 && images.has(Integer.toString(i))) {
               JSONObject imageObj = images.getJSONObject(Integer.toString(i));

               if (imageObj.has("BIG") && !imageObj.getString("BIG").isEmpty()) {
                  String image = homePageHttps + imageObj.getString("BIG");

                  if (image.contains("img")) {
                     primaryImage = homePageHttps + imageObj.getString("BIG");
                  }
               } else if (imageObj.has("MEDIUM") && !imageObj.getString("MEDIUM").isEmpty()) {
                  String image = homePageHttps + imageObj.getString("MEDIUM");

                  if (image.contains("img")) {
                     primaryImage = homePageHttps + imageObj.getString("MEDIUM");
                  }
               } else if (imageObj.has("SMALL") && !imageObj.getString("SMALL").isEmpty()) {
                  String image = homePageHttps + imageObj.getString("SMALL");

                  if (image.contains("img")) {
                     primaryImage = homePageHttps + imageObj.getString("SMALL");
                  }
               }
            }

            if (primaryImage != null) {
               break;
            }
         }
      }

      return primaryImage;
   }

   private List<String> crawlSecondaryImages(JSONObject json, String primaryImage) {
      List<String> secondaryImagesArray = new ArrayList<>();

      String primaryImageId = getImageId(primaryImage);

      if (json.has("mapOfImages")) {
         JSONObject images = json.getJSONObject("mapOfImages");

         for (String key : images.keySet()) { // index 0 may be a primary Image
            JSONObject imageObj = images.getJSONObject(key);

            if (imageObj.has("BIG") && !imageObj.getString("BIG").isEmpty()) {
               String image = homePageHttps + imageObj.getString("BIG");
               String imageId = getImageId(image);

               if (image.contains("img") && !imageId.equals(primaryImageId)) {
                  secondaryImagesArray.add(homePageHttps + imageObj.getString("BIG"));
               }
            } else if (imageObj.has("MEDIUM") && !imageObj.getString("MEDIUM").isEmpty()) {
               String image = homePageHttps + imageObj.getString("MEDIUM");
               String imageId = getImageId(image);

               if (image.contains("img") && !imageId.equals(primaryImageId)) {
                  secondaryImagesArray.add(homePageHttps + imageObj.getString("MEDIUM"));
               }
            } else if (imageObj.has("SMALL") && !imageObj.getString("SMALL").isEmpty()) {
               String image = homePageHttps + imageObj.getString("SMALL");
               String imageId = getImageId(image);

               if (image.contains("img") && !imageId.equals(primaryImageId)) {
                  secondaryImagesArray.add(homePageHttps + imageObj.getString("SMALL"));
               }
            }
         }
      }

      return secondaryImagesArray;
   }

   private String getImageId(String imageUrl) {
      if (imageUrl != null) {
         return imageUrl.replace(homePageHttps, "").split("/")[4];
      }

      return null;
   }

   private CategoryCollection crawlCategories(JSONObject json) {
      CategoryCollection categories = new CategoryCollection();

      if (json.has("shelfList")) {
         JSONArray shelfList = json.getJSONArray("shelfList");

         Set<String> listCategories =
            new HashSet<>(); // It is a "set" because it has been noticed that there are repeated
         // categories

         // The category fetched by crawler can be in a different ordination than showed on the website
         // and
         // its depends of each product.
         if (shelfList.length() > 0) {
            JSONObject cat1 = shelfList.getJSONObject(0);
            JSONObject cat2 = shelfList.getJSONObject(shelfList.length() - 1);

            if (cat1.has("name")) {
               listCategories.add(cat1.getString("name"));
            }

            if (cat2.has("name")) {
               listCategories.add(cat2.getString("name"));
            }
         }

         for (String category : listCategories) {
            categories.add(category);
         }
      }

      return categories;
   }

   private String crawlDescription(JSONObject json, String internalId) {
      StringBuilder description = new StringBuilder();
      String attributesDescription = JSONUtils.getStringValue(json, "description");

      if (attributesDescription != null) {
         description.append(attributesDescription);
      }

      if (json.has("shortDescription") && json.get("shortDescription") instanceof String) {
         description.append(json.getString("shortDescription"));
      }

      // Ex: https://www.paodeacucar.com/produto/329137
      if (json.has("itemMap")) {
         JSONArray itemMap = json.getJSONArray("itemMap");

         if (itemMap.length() > 0) {
            description.append(
               "<table class=\"nutritional-table table product-table\">\n"
                  + "                                <thead>\n"
                  + "                                    <tr>\n"
                  + "                                        <th colspan=\"2\" class=\"title\">Produtos no kit</th>\n"
                  + "                                    </tr>\n"
                  + "                                    <tr>\n"
                  + "                                        <th>Nome</th>\n"
                  + "                                        <th>Quantidade</th>\n"
                  + "                                    </tr>\n"
                  + "                                </thead>\n"
                  + "                                <tbody>\n");
            for (int i = 0; i < itemMap.length(); i++) {
               JSONObject productInfo = itemMap.getJSONObject(i);

               if (productInfo.has("quantity")
                  && productInfo.get("quantity") instanceof Integer
                  && productInfo.has("name")) {
                  int quantity = productInfo.getInt("quantity");
                  String name = productInfo.get("name").toString();

                  if (quantity > 1 || itemMap.length() > 1) {
                     description.append(
                        "<tr ng-repeat=\"item in productDetailCtrl.product.itemMap\" class=\"ng-scope\">\n"
                           + "        <td ng-class=\"{'last':$last}\" class=\"ng-binding last\">"
                           + name
                           + "l</td>\n"
                           + "        <td ng-class=\"{'last':$last}\" class=\"ng-binding last\">"
                           + quantity
                           + "</td>\n"
                           + "     </tr><!-- end ngRepeat: item in productDetailCtrl.product.itemMap -->\n");
                  }
               }
            }

            description.append("</tbody>\n</table>");
         }
      }

      // This key in json has a map of attributes -> {label: "", value = ""} , For crawl niutritional
      // table we make the html and put the values in html
      if (json.has("nutritionalMap") && json.getJSONObject("nutritionalMap").length() > 0) {
         JSONObject nutritionalJson = json.getJSONObject("nutritionalMap");

         StringBuilder str = new StringBuilder();

         str.append(
            "<div class=\"product-nutritional-table\">\n"
               + "  <p class=\"title\">Tabela nutricional</p>\n"
               + "   <!-- ngIf: productDetailCtrl.product.nutritionalMap.cabecalho -->"
               + "<div class=\"main-infos ng-scope\" ng-if=\"productDetailCtrl.product.nutritionalMap.cabecalho\">\n"
               + "           <p ng-bind-html=\"productDetailCtrl.product.nutritionalMap.cabecalho || "
               + "productDetailCtrl.product.nutritionalMap.cabecalho.value\" class=\"ng-binding\"></p>\n"
               + "       </div><!-- end ngIf: productDetailCtrl.product.nutritionalMap.cabecalho -->\n"
               + "       <table class=\"table table-responsive\">\n"
               + "         <thead>\n"
               + "              <tr>\n"
               + "                 <th>Item</th>\n"
               + "                   <th>Quantidade por porção</th>\n"
               + "                   <th>Valores diários</th>\n"
               + "             </tr>\n"
               + "           </thead>\n");
         str.append(crawlNutritionalTableAttributes(nutritionalJson));
         str.append("</table>\n</div>");

         description.append(str.toString());
      }

      description.append(CrawlerUtils.scrapStandoutDescription("gpa", session, cookies, dataFetcher));
      description.append(CrawlerUtils.scrapLettHtml(internalId, session, store.equals("pa") ? 4 : 5));

      return description.toString();
   }

   private String crawlNutritionalTableAttributes(JSONObject nutritionalMap) {
      StringBuilder str = new StringBuilder();
      str.append("<tbody>");

      Set<String> attributesList = nutritionalMap.keySet();

      for (String attribute : attributesList) {
         if (!(nutritionalMap.get(attribute) instanceof String)) {
            JSONObject attributeJson = JSONUtils.getValueRecursive(nutritionalMap, "attributes.0", JSONObject.class);

            if (attributeJson != null && attributeJson.has("value") && attributeJson.has("label")) {
               str.append(
                  putAttribute(attributeJson.getString("value"), attributeJson.getString("label")));
            }
         } else {
            str.append(
               "<div class=\"main-infos ng-scope\" ng-if=\"productDetailCtrl.product.nutritionalMap.cabecalho\">\n"
                  + "<p ng-bind-html=\"productDetailCtrl.product.nutritionalMap.cabecalho "
                  + "|| productDetailCtrl.product.nutritionalMap.cabecalho.value\" class=\"ng-binding\">"
                  + nutritionalMap.getString(attribute)
                  + "</p>\n"
                  + "</div>");
         }
      }

      str.append("</tbody");
      return str.toString();
   }

   private String putAttribute(String value, String label) {
      if (label != null) {
         if (label.equalsIgnoreCase("rodape")) {
            return "<tfoot>\n"
               + "  <tr>\n"
               + "     <td colspan=\"3\" ng-bind-html=\"productDetailCtrl.product.nutritionalMap.rodape.value\""
               + "class=\"last ng-binding\">"
               + value
               + "</td>\n"
               + "  </tr>\n"
               + "</tfoot>\n";
         } else {
            return "    <tr ng-repeat=\"(key, item) in productDetailCtrl.product.nutritionalMap \" ng-if=\"[ 'cabecalho', 'rodape'].indexOf(key) === -1 \" class=\"ng-scope\">\n"
               + "     <td class=\"ng-binding\">"
               + label
               + "</td>\n"
               + "      <td class=\"ng-binding\">"
               + value
               + "</td>\n"
               + "     <td class=\"ng-binding\"></td>\n"
               + "   </tr><!-- end ngIf: [ 'cabecalho', 'rodape'].indexOf(key) === -1 --><!-- end ngRepeat: "
               + "(key, item) in productDetailCtrl.product.nutritionalMap --><!-- ngIf: [ 'cabecalho', 'rodape'].indexOf(key) === -1 -->"
               + "<tr ng-repeat=\"(key, item) in productDetailCtrl.product.nutritionalMap \" ng-if=\"[ 'cabecalho', 'rodape'].indexOf(key) === -1 "
               + "\" class=\"ng-scope\">\n";
         }
      }

      return "";
   }

   /**
    * In this site has no information of installments
    *
    * @param price
    * @return
    */
   private Prices crawlPrices(Float price, Double priceFrom) {
      Prices p = new Prices();

      if (price != null) {
         Map<Integer, Float> installmentPriceMap = new HashMap<>();
         installmentPriceMap.put(1, price);

         p.setPriceFrom(priceFrom);
         p.setBankTicketPrice(price);

         p.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
         p.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
         p.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
         p.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
         p.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
         p.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);
      }

      return p;
   }

   /**
    * Get the json of gpa api, this api has all info of product
    *
    * @return
    */
   private JSONObject crawlProductInformatioFromGPAApi(String productUrl) {
      JSONObject productsInfo = new JSONObject();

      String id = "";
      if (productUrl.startsWith(homePageHttps)) {
         id = productUrl.replace(homePageHttps, "").split("/")[1];
      }

      String url =
         END_POINT_REQUEST
            + this.store
            + "/v4/products/ecom/"
            + id
            + "/bestPrices"
            + "?isClienteMais=false";

      if (this.storeId != null) {
         url += "&storeId=" + this.storeId;
      }

      Request request = RequestBuilder.create()
         .setUrl(url).setCookies(cookies).build();
      String res = this.dataFetcher.get(session, request).getBody();

      JSONObject apiGPA = JSONUtils.stringToJson(res);
      if (apiGPA.optJSONObject("content") instanceof JSONObject) {
         productsInfo = apiGPA.optJSONObject("content");
      }

      return productsInfo;
   }

   /**
    * Number of ratings appear in key rating in json
    */
   private Integer getTotalNumOfReviews(JSONObject rating) {
      Integer totalReviews = 0;

      if (rating.has("rating")) {
         JSONObject ratingValues = rating.getJSONObject("rating");

         totalReviews = 0;

         for (int i = 1; i <= ratingValues.length(); i++) {
            if (ratingValues.has(Integer.toString(i))) {
               totalReviews += ratingValues.getInt(Integer.toString(i));
            }
         }
      }
      return totalReviews;
   }

   private boolean isProductPage(String url) {
      return url.contains(this.homePageHttps + "produto/");
   }

   protected RatingsReviews extractRatingAndReviews(String internalId) {
      RatingsReviews ratingReviews = new RatingsReviews();
      Request request =
         RequestBuilder.create()
            .setUrl(END_POINT_REQUEST + store + "/products/" + internalId + "/review")
            .build();
      JSONObject jsonObject = JSONUtils.stringToJson(dataFetcher.get(session, request).getBody());

      if (jsonObject.has("content")) {
         JSONObject rating = jsonObject.optJSONObject("content");

         if (isProductPage(session.getOriginalURL())) {

            ratingReviews.setDate(session.getDate());
            Integer totalNumOfEvaluations = rating.optInt("total", 0);
            Integer totalReviews = getTotalNumOfReviews(rating);
            Double avgRating = rating.optDouble("average", 0D);

            ratingReviews.setTotalRating(totalNumOfEvaluations);
            ratingReviews.setTotalWrittenReviews(totalReviews);
            ratingReviews.setAverageOverallRating(avgRating);
            ratingReviews.setInternalId(crawlInternalId(session.getOriginalURL()));

            AdvancedRatingReview advancedRatingReview = getTotalStarsFromEachValue(rating);
            ratingReviews.setAdvancedRatingReview(advancedRatingReview);
         }
      }
      return ratingReviews;
   }

   public static AdvancedRatingReview getTotalStarsFromEachValue(JSONObject rating) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      if (rating.has("rating")) {

         JSONObject histogram = rating.getJSONObject("rating");

         if (histogram.has("1") && histogram.get("1") instanceof Integer) {
            star1 = histogram.getInt("1");
         }

         if (histogram.has("2") && histogram.get("2") instanceof Integer) {
            star2 = histogram.getInt("2");
         }

         if (histogram.has("3") && histogram.get("3") instanceof Integer) {
            star3 = histogram.getInt("3");
         }

         if (histogram.has("4") && histogram.get("4") instanceof Integer) {
            star4 = histogram.getInt("4");
         }

         if (histogram.has("5") && histogram.get("5") instanceof Integer) {
            star5 = histogram.getInt("5");
         }
      }

      return new AdvancedRatingReview.Builder()
         .totalStar1(star1)
         .totalStar2(star2)
         .totalStar3(star3)
         .totalStar4(star4)
         .totalStar5(star5)
         .build();
   }

   private String crawlInternalId(String productUrl) {
      return CommonMethods.getLast(productUrl.replace(this.homePageHttps, "").split("produto/"))
         .split("/")[0];
   }

   private Offers scrapOffers(JSONObject data) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      if (data != null) {
         Pricing pricing = scrapPricing(data);
         String sales = CrawlerUtils.calculateSales(pricing);

         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(MAIN_SELLER_NAME)
            .setSales(Collections.singletonList(sales))
            .setMainPagePosition(1)
            .setSellersPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(JSONObject data) throws MalformedPricingException {
      Double spotlightPrice = null;
      Double priceFrom = null;

      if (data.has("productPromotions")) {
         JSONArray promotions = data.optJSONArray("productPromotions");
         for (Object e : promotions) {
            if (e instanceof JSONObject && ((JSONObject) e).optInt("ruleId") == 51241) {
               spotlightPrice = ((JSONObject) e).optDouble("unitPrice");
               priceFrom = data.optDouble("currentPrice");
            }
         }
      }
      if (spotlightPrice == null) {
         spotlightPrice = data.optDouble("currentPrice");
      }

      if (priceFrom == null && data.has("priceFrom")) {
         priceFrom = data.optDouble("priceFrom");
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(BankSlipBuilder.create()
            .setFinalPrice(spotlightPrice)
            .build())
         .build();

   }

   protected CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      for (String brand : cards) {
         creditCards.add(CreditCardBuilder.create()
            .setBrand(brand)
            .setIsShopCard(false)
            .setInstallments(installments)
            .build());
      }

      return creditCards;
   }


   private boolean hasMarketPlace(Document doc) {
      Elements sellerContainer = doc.select(".buy-box-contentstyles__Container-sc-18rwav0-2.grwTtk");
      String sellerName = CrawlerUtils.scrapStringSimpleInfo(doc,".buy-box-contentstyles__Container-sc-18rwav0-2.grwTtk p:first-child span:not(:first-child)", false);

      boolean equalsSeller = false;

      if(sellerName != null){
         equalsSeller = !sellerName.equalsIgnoreCase(MAIN_SELLER_NAME);
      }
      return !(sellerContainer.size() > 1) || equalsSeller;
   }

   private Offers offersFromMarketPlace(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      int pos = 1;

      Elements ofertas = doc.select(".buy-box-contentstyles__Container-sc-18rwav0-2.grwTtk");

      if (ofertas != null) {
         for (Element oferta : ofertas) {
            String sellerName = CrawlerUtils.scrapStringSimpleInfo(oferta, "p:first-child span:not(:first-child)", false);
            Pricing pricing = scrapSellersPricing(oferta);
            boolean isMainRetailer = sellerName.equalsIgnoreCase(MAIN_SELLER_NAME);

            offers.add(Offer.OfferBuilder.create()
               .setInternalSellerId(CommonMethods.toSlug(MAIN_SELLER_NAME))
               .setSellerFullName(sellerName)
               .setSellersPagePosition(pos)
               .setIsBuybox(false)
               .setIsMainRetailer(isMainRetailer)
               .setPricing(pricing)
               .build());

            pos++;
         }
      }
      return offers;
   }


   private Pricing scrapSellersPricing(Element e) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(e, ".current-pricesectionstyles__CurrentPrice-sc-17j9p6i-0 p", null, false, ',', session);
      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }
}
