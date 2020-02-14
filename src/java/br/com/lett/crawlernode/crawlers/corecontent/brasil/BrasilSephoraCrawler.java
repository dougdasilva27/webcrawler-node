package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

/**
 * date: 05/09/2018
 * 
 * @author gabriel
 *
 */

public class BrasilSephoraCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.sephora.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "sephora";

   public BrasilSephoraCrawler(Session session) {
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

         JSONObject chaordicJson = crawlChaordicJson(doc);

         String internalPid = crawlInternalPid(chaordicJson);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs li:not(.home, .product) a", false);
         String description = crawlDescription(doc);

         // sku data in json
         JSONArray arraySkus = chaordicJson != null && chaordicJson.has("offers") ? chaordicJson.getJSONArray("offers") : new JSONArray();

         for (int i = 0; i < arraySkus.length(); i++) {
            JSONObject jsonSku = arraySkus.getJSONObject(i);

            String internalId = crawlInternalId(jsonSku);
            Element variantElement = crawlVariationElement(doc, i);
            String name = variantElement != null ? crawlName(chaordicJson, variantElement)
                  : CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name h1", true);
            String primaryImage = crawlPrimaryImage(doc, variantElement);

            Map<String, Prices> marketplaceMap = crawlMarketplace(jsonSku, doc);
            boolean available = jsonSku.has("availability") && jsonSku.get("availability").toString().contains("InStock")
                  && marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);

            Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, Arrays.asList(MAIN_SELLER_NAME_LOWER), Card.VISA, session);
            Prices prices = marketplaceMap.get(MAIN_SELLER_NAME_LOWER);
            Float price = crawlPrice(prices);
            String secondaryImages = crawlSecondaryImages(doc);
            String ean = crawlEan(jsonSku);
            RatingsReviews ratingReviews = crawRating(doc);
            List<String> eans = new ArrayList<>();
            eans.add(ean);

            // Creating the product
            Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setPrice(price)
                  .setPrices(prices)
                  .setAvailable(available)
                  .setCategory1(categories.getCategory(0))
                  .setCategory2(categories.getCategory(1))
                  .setCategory3(categories.getCategory(2))
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setDescription(description)
                  .setMarketplace(marketplace)
                  .setEans(eans)
                  .setRatingReviews(ratingReviews)
                  .build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   /*******************************
    * Product page identification *
    *******************************/

   private boolean isProductPage(Document document) {
      return document.selectFirst(".reference") != null;
   }

   /*******************
    * General methods *
    *******************/

   private String crawlInternalId(JSONObject skuJson) {
      String internalId = null;

      if (skuJson.has("sku")) {
         internalId = skuJson.getString("sku").trim();
      }

      return internalId;
   }

   private String crawlInternalPid(JSONObject json) {
      String internalPid = null;

      if (json.has("sku")) {
         internalPid = json.get("sku").toString();
      }

      return internalPid;
   }

   private String crawlVariationId(Element variantElement) {
      return variantElement.attr("data-productid");
   }

   private Element crawlVariationElement(Document doc, int productPosition) {
      Element variantElement = new Element("<div></div>");

      Elements variations = doc.select("#product-info-grouped li[id]");

      if (variations.size() > productPosition) {
         variantElement = doc.selectFirst("#product-info-grouped li[id]:nth-child(" + (productPosition + 1) + ")");
      }

      return variantElement;
   }

   private String crawlName(JSONObject chaordicJson, Element variantElement) {
      StringBuilder name = new StringBuilder();

      if (chaordicJson.has("name")) {
         name.append(chaordicJson.getString("name"));

         Element nameV = variantElement.selectFirst("label p.reference.info");
         if (nameV != null) {
            name.append(" " + nameV.ownText());
         }

      }

      return name.toString();
   }

   private Float crawlPrice(Prices prices) {
      Float price = null;

      if (!prices.isEmpty() && prices.getCardPaymentOptions(Card.VISA.toString()).containsKey(1)) {
         Double priceDouble = prices.getCardPaymentOptions(Card.VISA.toString()).get(1);
         price = priceDouble.floatValue();
      }

      return price;
   }

   private String crawlPrimaryImage(Document doc, Element variantElement) {
      String primaryImage = null;

      Element image = doc.selectFirst("#image-main[data-zoom-image]");

      if (variantElement != null) {
         String variationImage = variantElement.attr("data-base-image").trim();
         String variantId = crawlVariationId(variantElement);
         Element variantImage = doc.selectFirst("#variant-" + variantId + "[data-zoom-image]");

         if (variantImage != null && !variationImage.isEmpty()) {
            image = variantImage;
         }
      }

      if (image != null) {
         primaryImage = CrawlerUtils.sanitizeUrl(image, Arrays.asList("data-zoom-image", "src"), "https:", "sephora-resize.s3.amazonaws.com");
      }

      return primaryImage;
   }

   private String crawlSecondaryImages(Document doc) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      Elements images = doc.select("#container_gallery .link-media-gallery");

      for (int i = 1; i < images.size(); i++) {
         Element e = images.get(i);

         String image = e.attr("data-zoom-image").trim();

         if (image.isEmpty()) {
            image = e.attr("src");
         }

         if (!image.startsWith("http")) {
            image = "https:" + image;
         }

         secondaryImagesArray.put(image);
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   private Map<String, Prices> crawlMarketplace(JSONObject jsonSku, Document doc) {
      Map<String, Prices> marketplace = new HashMap<>();

      String sellerName = MAIN_SELLER_NAME_LOWER;

      if (jsonSku.has("seller")) {
         JSONObject sellerJson = jsonSku.getJSONObject("seller");

         if (sellerJson.has("name")) {
            sellerName = sellerJson.getString("name").toLowerCase();
         }
      }

      marketplace.put(sellerName, crawlPrices(jsonSku, doc));

      return marketplace;

   }

   private String crawlDescription(Document doc) {
      StringBuilder description = new StringBuilder();

      Elements elementsInformation = doc.select("#section-description, #neemu-how-to-use, #neemu-look, .info-content#brand");
      for (Element e : elementsInformation) {
         description.append(e.html());
      }

      return description.toString();
   }

   /**
    * To crawl installments in this site, we need crawl the installments array rule, like this:
    * [["40","1",""],["60","2",""],["80","3",""],["100","4",""],["120","5",""],["140","6",""],["160","7",""],["180","8",""],["200","9",""],["","10",""]]
    * 
    * I found this array because in this site, the installments values are calculated, so
    * 
    * 
    * - if the price is lower then 40 will have 1 installment
    * 
    * - if the price is lower then 60 and greater then 40 will have 2 installments
    * 
    * ...
    *
    * @param price
    * @param jsonSku
    * @return
    */
   private Prices crawlPrices(JSONObject jsonSku, Document doc) {
      Prices prices = new Prices();

      if (jsonSku.has("price")) {
         Float price = CrawlerUtils.getFloatValueFromJSON(jsonSku, "price", true, false);
         prices.setBankTicketPrice(price);

         Map<Integer, Float> mapInstallments = new HashMap<>();
         mapInstallments.put(1, price);

         Map<Float, Integer> installmentsRules = crawlInstallmentsRules(doc);

         Integer installmentsNumber = 1;
         for (Entry<Float, Integer> entry : installmentsRules.entrySet()) {
            if (price < entry.getKey()) {
               installmentsNumber = entry.getValue();
               break;
            }
         }

         mapInstallments.put(installmentsNumber, MathUtils.normalizeTwoDecimalPlaces(price / installmentsNumber));

         prices.insertCardInstallment(Card.VISA.toString(), mapInstallments);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), mapInstallments);
         prices.insertCardInstallment(Card.AMEX.toString(), mapInstallments);
         prices.insertCardInstallment(Card.DINERS.toString(), mapInstallments);
         prices.insertCardInstallment(Card.HIPERCARD.toString(), mapInstallments);
      }

      return prices;
   }

   private Map<Float, Integer> crawlInstallmentsRules(Document doc) {
      Map<Float, Integer> installmentsRulesMap = new TreeMap<>();

      Elements scripts = doc.select("script");
      for (Element e : scripts) {
         String script = e.html().replace(" ", "").toLowerCase();

         if (script.contains("newcatalogproduct(")) {
            int x = script.indexOf("[[");
            int y = script.indexOf("]]", x) + 2;

            try {
               JSONArray array = new JSONArray(script.substring(x, y));

               for (Object o : array) {
                  JSONArray installments = (JSONArray) o;

                  if (installments.length() > 1) {
                     Float value = MathUtils.parseFloatWithComma(installments.get(0).toString());
                     Integer installmentNumber = MathUtils.parseInt(installments.get(1).toString());

                     if (value != null && installmentNumber != null) {
                        installmentsRulesMap.put(value, installmentNumber);
                     } else if (installmentNumber != null) {
                        installmentsRulesMap.put(999999999f, installmentNumber);
                     }
                  }
               }
            } catch (JSONException e1) {
               Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e1));
            }

            break;
         }
      }

      return installmentsRulesMap;
   }

   private JSONObject crawlChaordicJson(Document doc) {
      JSONObject skuJson = new JSONObject();

      Elements scripts = doc.select("script[type=\"application/ld+json\"]");

      for (Element e : scripts) {
         String script = e.html().trim();

         if (script.contains("sku") && script.startsWith("[") && script.endsWith("]")) {
            try {
               JSONArray array = new JSONArray(script);

               if (array.length() > 0) {
                  skuJson = array.getJSONObject(0);
               }
            } catch (Exception e1) {
               Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e1));
            }

            break;
         }
      }

      return skuJson;
   }

   private String crawlEan(JSONObject json) {
      String ean = null;

      if (json.has("gtin13") && json.get("gtin13") instanceof String) {
         ean = json.getString("gtin13");
      }

      return ean;
   }

   private RatingsReviews crawRating(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = getTotalNumOfRatings(doc);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(getTotalAvgRating(doc));

      return ratingReviews;
   }

   private Double getTotalAvgRating(Document docRating) {
      Double avgRating = 0d;
      Element rating = docRating.selectFirst("#customer-reviews .average span");

      if (rating != null) {
         String text = rating.ownText().replaceAll("[^0-9.]", "").trim();

         if (!text.isEmpty()) {
            avgRating = Double.parseDouble(text);
         }
      }

      return avgRating;
   }


   private Integer getTotalNumOfRatings(Document doc) {
      Integer totalRating = 0;
      Element totalRatingElement = doc.selectFirst("#customer-reviews .average");

      if (totalRatingElement != null) {
         String totalText = totalRatingElement.ownText().replaceAll("[^0-9]", "").trim();

         if (!totalText.isEmpty()) {
            totalRating = Integer.parseInt(totalText);
         }
      }

      return totalRating;
   }

}
