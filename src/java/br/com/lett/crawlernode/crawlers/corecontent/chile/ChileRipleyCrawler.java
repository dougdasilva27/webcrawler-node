package br.com.lett.crawlernode.crawlers.corecontent.chile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
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
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

/**
 * Date: 22/10/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class ChileRipleyCrawler extends Crawler {

   private static final String HOME_PAGE = "https://simple.ripley.cl/";
   private static final String SELLER_NAME_LOWER = "ripley";

   public ChileRipleyCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   private Map<Float, Map<Integer, Float>> installmentsMap = new HashMap<>();

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

         JSONObject productJson = extractProductJson(doc);

         String internalPid = crawlInternalPid(productJson);
         CategoryCollection categories = new CategoryCollection();
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#descripcion", "#especificaciones", ".product-general-info"));

         JSONArray arraySkus = productJson.has("SKUs") ? productJson.getJSONArray("SKUs") : new JSONArray();

         for (int i = 0; i < arraySkus.length(); i++) {
            JSONObject skuJson = arraySkus.getJSONObject(i);
            String internalId = crawlInternalId(skuJson);
            String name = crawlName(productJson, skuJson);

            JSONObject prodAPI = fetchProductAPI(internalId);
            Map<String, Prices> marketplaceMap = crawlMarketplaceMap(prodAPI, skuJson);
            Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, Arrays.asList(SELLER_NAME_LOWER), Card.AMEX, session);
            boolean available = marketplaceMap.containsKey(SELLER_NAME_LOWER);
            Integer stock = available ? crawlStock(skuJson) : 0;
            Prices prices = CrawlerUtils.getPrices(marketplaceMap, Arrays.asList(SELLER_NAME_LOWER));
            Float price = CrawlerUtils.extractPriceFromPrices(prices, Arrays.asList(Card.AMEX, Card.SHOP_CARD));
            String primaryImage = prodAPI.has("fullImage") ? CrawlerUtils.completeUrl(prodAPI.getString("fullImage"), "https:", "home.ripley.cl") : null;
            String secondaryImages = crawlSecondaryImages(prodAPI.has("images") ? prodAPI.getJSONArray("images") : new JSONArray(), primaryImage);
            RatingsReviews ratingsReviews = scrapRatingReviews(doc);

            // Creating the product
            Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
                  .setPrice(price).setPrices(prices).setAvailable(available && price != null).setCategory1(categories.getCategory(0))
                  .setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages).setDescription(description).setStock(stock).setRatingReviews(ratingsReviews).setMarketplace(marketplace).build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   /**
    * 
    * @param doc
    * @return
    */
   private boolean isProductPage(Document doc) {
      return !doc.select(".product-item").isEmpty();
   }

   /**
    * 
    * @param doc
    * @return
    */
   private JSONObject extractProductJson(Document doc) {
      JSONObject productJson = new JSONObject();

      JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.__PRELOADED_STATE__ =", ";", false, true);
      if (json.has("product")) {
         JSONObject state = json.getJSONObject("product");

         if (state.has("product")) {
            productJson = state.getJSONObject("product");
         }
      }

      return productJson;
   }

   private Double scrapAvgRating(Document doc) {
      Double avg = 0d;
      Double avgRating = 0d;
      Integer ratingReviews = 0;
      Elements ratings = doc.select(".pr-review-summary article");
      Elements reviews = doc.select(".pr-review-summary article div[itemprop] span");


      for (Element rating : ratings) {
         avgRating++;
      }

      for (Element review : reviews) {

         ratingReviews += review != null ? Integer.parseInt(review.text()) : 0;
      }

      avg = ratingReviews / avgRating;

      return avg;
   }

   private Integer scrapTotalComments(Document doc) {
      Integer totalComments = 0;
      Elements comments = doc.select(".pr-review-summary article");

      for (Element comment : comments) {
         totalComments++;
      }

      return totalComments;
   }

   private RatingsReviews scrapRatingReviews(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalComments = scrapTotalComments(doc);
      Double avgRating = scrapAvgRating(doc);
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(doc);

      ratingReviews.setTotalRating(totalComments);
      ratingReviews.setTotalWrittenReviews(totalComments);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = doc.select(".pr-review-summary article div[itemprop] span");



      for (Element review : reviews) {

         Integer val1 = review != null ? Integer.parseInt(review.text()) : 0;


         // On a html this value will be like this: (1)


         switch (val1) {
            case 5:
               star5++;
               break;
            case 4:
               star4++;
               break;
            case 3:
               star3++;
               break;
            case 2:
               star2++;
               break;
            case 1:
               star1++;
               break;
            default:
               break;
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

   /**
    * @param skuJson
    * @return
    */
   private Integer crawlStock(JSONObject skuJson) {
      Integer stock = null;

      if (skuJson.has("xCatEntryQuantity")) {
         stock = skuJson.getInt("xCatEntryQuantity");
      }

      return stock;
   }

   private String crawlInternalId(JSONObject skuJson) {
      String internalId = null;

      if (skuJson.has("SKUUniqueID")) {
         internalId = skuJson.getString("SKUUniqueID");
      }

      return internalId;
   }

   private String crawlInternalPid(JSONObject productJson) {
      String internalPid = null;

      if (productJson.has("partNumber")) {
         internalPid = productJson.get("partNumber").toString();
      }

      return internalPid;
   }

   private String crawlName(JSONObject productJson, JSONObject skuJson) {
      StringBuilder name = new StringBuilder();

      if (productJson.has("name")) {
         name.append(productJson.get("name"));

         if (skuJson.has("Attributes")) {
            JSONArray attributes = skuJson.getJSONArray("Attributes");

            for (Object o : attributes) {
               JSONObject attribute = (JSONObject) o;

               if (attribute.has("name")) {
                  String nameAtt = attribute.getString("name");

                  if (nameAtt.equalsIgnoreCase("seller") || nameAtt.equalsIgnoreCase("OFFER_STATE")) {
                     continue;
                  }
               }

               if (attribute.has("Values")) {
                  JSONArray values = attribute.getJSONArray("Values");

                  for (Object obj : values) {
                     JSONObject value = (JSONObject) obj;

                     if (value.has("values")) {
                        name.append(" ").append(value.get("values"));
                     }
                  }
               }
            }
         }
      }

      return name.toString();
   }

   private String crawlSecondaryImages(JSONArray images, String primaryImage) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      for (Object o : images) {
         String image = CrawlerUtils.completeUrl((String) o, "https:", "home.ripley.cl");

         if (!image.equalsIgnoreCase(primaryImage)) {
            secondaryImagesArray.put(image);
         }
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   private Prices crawlPrices(JSONObject productApi) {
      Prices prices = new Prices();

      if (productApi.has("prices")) {
         JSONObject pricesJson = productApi.getJSONObject("prices");
         boolean hasCardInstallment = false;

         if (pricesJson.has("listPrice")) {
            prices.setPriceFrom(CrawlerUtils.getDoubleValueFromJSON(pricesJson, "listPrice"));
         }

         if (pricesJson.has("offerPrice")) {
            Float value = CrawlerUtils.getFloatValueFromJSON(pricesJson, "offerPrice");

            if (value != null) {
               Map<Integer, Float> mapInstallments = new HashMap<>();

               if (this.installmentsMap.containsKey(value)) {
                  mapInstallments.putAll(this.installmentsMap.get(value));
               } else {
                  mapInstallments.put(1, value);

                  Float value3x = setInstalmentsValeuFromAPI(3, value);
                  if (value3x != null) {
                     mapInstallments.put(3, value3x);
                  }

                  Float value48x = setInstalmentsValeuFromAPI(48, value);
                  if (value48x != null) {
                     mapInstallments.put(48, value48x);
                  }

                  this.installmentsMap.put(value, mapInstallments);
               }

               hasCardInstallment = true;
               prices.insertCardInstallment(Card.AMEX.toString(), mapInstallments);
            }
         }

         if (pricesJson.has("cardPrice")) {
            Float value = CrawlerUtils.getFloatValueFromJSON(pricesJson, "cardPrice");

            if (value != null) {
               Map<Integer, Float> mapInstallmentsShopCard = new HashMap<>();

               if (this.installmentsMap.containsKey(value)) {
                  mapInstallmentsShopCard.putAll(this.installmentsMap.get(value));
               } else {
                  mapInstallmentsShopCard.put(1, value);

                  Float value3x = setInstalmentsValeuFromAPI(3, value);
                  if (value3x != null) {
                     mapInstallmentsShopCard.put(3, value3x);
                  }

                  Float value48x = setInstalmentsValeuFromAPI(48, value);
                  if (value48x != null) {
                     mapInstallmentsShopCard.put(48, value48x);
                  }

                  this.installmentsMap.put(value, mapInstallmentsShopCard);
               }

               hasCardInstallment = true;
               prices.insertCardInstallment(Card.SHOP_CARD.toString(), mapInstallmentsShopCard);
            }
         }

         if (!hasCardInstallment) {
            prices = new Prices();
         }
      }

      return prices;

   }

   private Float setInstalmentsValeuFromAPI(Integer installment, Float totalValue) {
      Float value = null;

      String url = "https://simple.ripley.cl/api/v1/products/instalment-simulation?instalments=" + installment + "&amount=" + totalValue.intValue();
      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      JSONObject installmentJson = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

      if (installmentJson.has("instalmentCost")) {
         value = CrawlerUtils.getFloatValueFromJSON(installmentJson, "instalmentCost");
      }

      return value;
   }

   private Map<String, Prices> crawlMarketplaceMap(JSONObject productApi, JSONObject skuJson) {
      Map<String, Prices> marketplaceMap = new HashMap<>();

      Integer stock = crawlStock(skuJson);

      if (stock > 0) {
         if (productApi.has("marketplace")) {
            JSONObject marketplace = productApi.getJSONObject("marketplace");

            if (marketplace.has("shopName")) {
               marketplaceMap.put(marketplace.get("shopName").toString().toLowerCase().trim(), crawlPrices(productApi));
            }
         }

         if (marketplaceMap.isEmpty()) {
            marketplaceMap.put(SELLER_NAME_LOWER, crawlPrices(productApi));
         }
      }

      return marketplaceMap;
   }

   public JSONObject fetchProductAPI(String internalId) {
      String url = "https://simple.ripley.cl/product/information/" + internalId;

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).build();
      return CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
   }
}
