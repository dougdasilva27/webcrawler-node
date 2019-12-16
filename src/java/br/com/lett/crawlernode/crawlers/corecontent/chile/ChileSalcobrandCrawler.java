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
import br.com.lett.crawlernode.util.MathUtils;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

public class ChileSalcobrandCrawler extends Crawler {

   public ChileSalcobrandCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = scrapInternalPid(doc);
         JSONArray skusStock = fetchStockAPI(internalPid);
         JSONObject skusPrices = CrawlerUtils.selectJsonFromHtml(doc, "script", "var prices = ", ";", false, true);
         String description = crawlDesciption(doc);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li  a:not([href=\"/\"])");

         // Json skusPrices Ex:
         // {"4750152":{"normal":"$3.299","oferta":null,"internet":null,"tarjeta":null},"4750155":{"normal":"$5.599","oferta":null,"internet":null,"tarjeta":null}}
         // When 4750152 and 4750155 are internalId's
         for (String internalId : skusPrices.keySet()) {

            String name = crawlName(doc, internalId);
            Integer stock = scrapStock(internalId, skusStock);
            boolean available = stock > 0;
            Float price = crawlPrice(skusPrices, internalId);
            Prices prices = crawlPrices(skusPrices, internalId);
            String primaryImage =
                  CrawlerUtils.scrapSimplePrimaryImage(doc, "img[alt^=" + internalId + "]", Arrays.asList("data-src", "src"), "https:", "salcobrand.cl");
            String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "img[alt^=" + internalId + "]", Arrays.asList("data-src", "src"),
                  "https:", "salcobrand.cl", primaryImage);
            String url = buildUrl(session.getOriginalURL(), internalId);
            RatingsReviews ratingsReviews = scrapRatingReviews(doc);

            Product product = ProductBuilder.create()
                  .setUrl(url)
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
                  .setMarketplace(new Marketplace())
                  .setRatingReviews(ratingsReviews)
                  .setStock(stock)
                  .build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private Integer scrapStock(String internalId, JSONArray skusStock) {
      Integer stock = 0;

      for (Object obj : skusStock) {
         JSONObject skuStock = (JSONObject) obj;

         if (skuStock.has(internalId)) {
            stock = CrawlerUtils.getIntegerValueFromJSON(skuStock, internalId, 0);
            break;
         }
      }

      return stock;
   }

   /**
    * Ex:
    * 
    * [{"4751790":0},{"4751792":1548}]
    * 
    * @param internalPid
    * @return
    */
   private JSONArray fetchStockAPI(String internalPid) {
      String url = "https://salcobrand.cl/api/v1/stock?sku=" + internalPid;
      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();

      return CrawlerUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = doc.select(".reviews > li");
      Elements stars = doc.select(".review > ul  > li .fa.fa-star.selected");

      int val1;
      for (Element review : reviews) {
         val1 = 0;
         for (Element i : stars) {
            val1++;
         }
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

         // On a html this value will be like this: (1)



      }

      return new AdvancedRatingReview.Builder()
            .totalStar1(star1)
            .totalStar2(star2)
            .totalStar3(star3)
            .totalStar4(star4)
            .totalStar5(star5)
            .build();
   }

   private RatingsReviews scrapRatingReviews(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalComments = scrapTotalComents(doc);
      Double avgRating = scrapAvgRating(doc);
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(doc);

      ratingReviews.setTotalRating(totalComments);
      ratingReviews.setTotalWrittenReviews(totalComments);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private Integer scrapTotalComents(Document doc) {
      Integer total = 0;
      Elements comentsReviews = doc.select(".reviews span p");
      for (Element el : comentsReviews) {
         total++;
      }

      return total;
   }

   private Double scrapAvgRating(Document doc) {
      Double avg = 0d;

      Elements reviews = doc.select(".reviews > li");
      Elements stars = doc.select(".review > ul  > li .fa.fa-star.selected");

      int val1 = 0;
      int val2 = 0;
      for (Element review : reviews) {
         val2++;
         for (Element i : stars) {
            val1++;
         }
      }
      avg = (double) (val1 / val2);

      return avg;
   }

   private String scrapInternalPid(Document doc) {
      String internalPid = null;

      String token = "\"sku=";

      Elements scripts = doc.select("script");
      for (Element e : scripts) {
         String text = e.html().toLowerCase().replace(" ", "");

         if (text.contains(token)) {
            int x = text.indexOf(token) + token.length();

            String id = text.substring(x).trim();
            if (id.contains("\"")) {
               int y = id.indexOf('"');

               internalPid = id.substring(0, y);
            } else {
               internalPid = id;
            }

            break;
         }
      }

      return internalPid;
   }

   private String crawlName(Document doc, String internalId) {
      StringBuilder name = new StringBuilder();
      name.append(CrawlerUtils.scrapStringSimpleInfo(doc, ".product-content .info", false));

      Element selectElement = doc.selectFirst("#variant_id option[sku=" + internalId + "]");
      if (selectElement != null) {
         name.append(" ").append(selectElement.text().trim());
         Element quantityNameElement = doc.selectFirst(".input-group .first option[data-values-ids~=" + selectElement.val() + "]");

         if (quantityNameElement != null) {
            name.append(" ").append(quantityNameElement.text());
         }
      }

      return name.toString();
   }


   private String crawlDesciption(Document doc) {
      String description = null;
      List<String> selectors = new ArrayList<>();

      selectors.add(".description");
      selectors.add("#description .description-area");
      description = CrawlerUtils.scrapSimpleDescription(doc, selectors);

      return description;
   }

   private Float crawlPrice(JSONObject jsonPrices, String internalId) {
      Float price = null;

      if (jsonPrices.has(internalId)) {
         JSONObject priceObj = jsonPrices.getJSONObject(internalId);

         if (priceObj.has("normal") && priceObj.has("oferta")) {

            if (!priceObj.isNull("oferta")) {
               price = MathUtils.parseFloatWithComma(priceObj.get("oferta").toString().trim());

            } else {
               price = MathUtils.parseFloatWithComma(priceObj.get("normal").toString().trim());
            }
         }
      }

      return price;
   }

   private Prices crawlPrices(JSONObject jsonPrices, String internalId) {
      Prices prices = new Prices();
      Map<Integer, Float> installments = new HashMap<>();

      if (jsonPrices.has(internalId)) {
         JSONObject priceObj = jsonPrices.getJSONObject(internalId);

         if (priceObj.has("normal")) {
            prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceObj.get("normal").toString().trim()));

         }

         if (priceObj.has("tarjeta") && !priceObj.isNull("tarjeta")) {
            installments.put(1, MathUtils.parseFloatWithComma(priceObj.get("tarjeta").toString().trim()));

         } else if (priceObj.has("oferta") && !priceObj.isNull("oferta")) {
            installments.put(1, MathUtils.parseFloatWithComma(priceObj.get("oferta").toString().trim()));

         } else if (priceObj.has("normal") && !priceObj.isNull("normal")) {
            installments.put(1, MathUtils.parseFloatWithComma(priceObj.get("normal").toString().trim()));

         }
      }

      if (!installments.isEmpty()) {
         prices.insertCardInstallment(Card.VISA.toString(), installments);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installments);
      }

      return prices;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".big-product-container") != null;
   }

   private String buildUrl(String url, String productId) {
      String finalUrl = url;

      if (finalUrl != null) {
         finalUrl = finalUrl.substring(0, finalUrl.lastIndexOf('=') + 1) + productId;
      }

      return finalUrl;
   }
}
