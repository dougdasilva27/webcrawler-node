package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
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
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

/**
 * Date: 07/12/2016
 * 
 * 1) Only one sku per page. 2) Is required put parameter sc with value 15 to access product url 3)
 * There is no informations of installments in this market 4) In time this crawler was made, it was
 * not foundo unnavailable products
 * 
 * @author Gabriel Dornelas
 *
 */
public class ArgentinaWalmartCrawler extends Crawler {

   private final String HOME_PAGE = "http://www.walmart.com.ar/";

   public ArgentinaWalmartCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   /**
    * To acess product page is required put ?sc=15 in url
    */
   @Override
   public String handleURLBeforeFetch(String curURL) {

      if (curURL.endsWith("/p")) {

         try {
            String url = curURL;
            List<NameValuePair> paramsOriginal = URLEncodedUtils.parse(new URI(url), "UTF-8");
            List<NameValuePair> paramsNew = new ArrayList<>();

            for (NameValuePair param : paramsOriginal) {
               if (!"sc".equals(param.getName())) {
                  paramsNew.add(param);
               }
            }

            paramsNew.add(new BasicNameValuePair("sc", "15"));
            URIBuilder builder = new URIBuilder(curURL.split("\\?")[0]);

            builder.clearParameters();
            builder.setParameters(paramsNew);

            curURL = builder.build().toString();

            return curURL;

         } catch (URISyntaxException e) {
            return curURL;
         }
      }

      return curURL;

   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {

      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = crawlInternalPid(doc);
         CategoryCollection categories = crawlCategories(doc);
         String description = crawlDescription(doc);
         Integer stock = null;
         RatingsReviews ratingReviews = scrapRatingReviews(doc);
         Map<String, Float> marketplaceMap = crawlMarketplace();
         Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap);
         JSONArray arraySkus = crawlSkuJsonArray(doc);
         JSONArray eanArray = CrawlerUtils.scrapEanFromVTEX(doc);

         for (int i = 0; i < arraySkus.length(); i++) {
            JSONObject jsonSku = arraySkus.getJSONObject(i);

            boolean available = crawlAvailability(jsonSku);
            String internalId = crawlInternalId(jsonSku);
            Float price = crawlMainPagePrice(jsonSku, available);
            String primaryImage = crawlPrimaryImage(doc);
            String name = crawlName(doc, jsonSku);
            String secondaryImages = crawlSecondaryImages(doc);
            Prices prices = crawlPrices(price);
            String ean = i < eanArray.length() ? eanArray.getString(i) : null;
            List<String> eans = new ArrayList<>();
            eans.add(ean);

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
                  .setStock(stock)
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

   private boolean isProductPage(Document document) {
      if (document.select(".productName").first() != null) {
         return true;
      }
      return false;
   }

   private String crawlInternalId(JSONObject json) {
      String internalId = null;

      if (json.has("sku")) {
         internalId = Integer.toString(json.getInt("sku")).trim();
      }

      return internalId;
   }

   private String crawlInternalPid(Document document) {
      String internalPid = null;
      Element internalPidElement = document.select("#___rc-p-id").first();

      if (internalPidElement != null) {
         internalPid = internalPidElement.attr("value").toString().trim();
      }

      return internalPid;
   }

   private String crawlName(Document document, JSONObject jsonSku) {
      String name = null;
      Element nameElement = document.select(".productName").first();

      String nameVariation = jsonSku.getString("skuname");

      if (nameElement != null) {
         name = nameElement.text().toString().trim();

         if (name.length() > nameVariation.length()) {
            name += " " + nameVariation;
         } else {
            name = nameVariation;
         }
      }

      return name;
   }

   private Float crawlMainPagePrice(JSONObject json, boolean available) {
      Float price = null;

      if (json.has("bestPriceFormated") && available) {
         price = Float.parseFloat(json.getString("bestPriceFormated").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
      }

      return price;
   }

   private boolean crawlAvailability(JSONObject json) {
      if (json.has("available")) {
         return json.getBoolean("available");
      }
      return false;
   }

   private Map<String, Float> crawlMarketplace() {
      return new HashMap<>();
   }

   private Marketplace assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
      return new Marketplace();
   }

   private String crawlPrimaryImage(Document doc) {
      String primaryImage = null;

      Element image = doc.select("#botaoZoom").first();

      if (image != null) {
         primaryImage = image.attr("zoom").trim();

         if (primaryImage == null || primaryImage.isEmpty()) {
            primaryImage = image.attr("rel").trim();
         }
      }

      return primaryImage;
   }

   private String crawlSecondaryImages(Document doc) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      Elements imageThumbs = doc.select("#botaoZoom");

      for (int i = 1; i < imageThumbs.size(); i++) { // starts with index 1, because the first image is the primary image
         String url = imageThumbs.get(i).attr("zoom");

         if (url == null || url.isEmpty()) {
            url = imageThumbs.get(i).attr("rel");
         }

         if (url != null && !url.isEmpty()) {
            secondaryImagesArray.put(url);
         }
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   private CategoryCollection crawlCategories(Document document) {
      CategoryCollection categories = new CategoryCollection();
      Elements elementCategories = document.select(".bread-crumb > ul li a");

      for (int i = 1; i < elementCategories.size(); i++) { // starting from index 1, because the first is the market name
         categories.add(elementCategories.get(i).text().trim());
      }

      return categories;
   }

   private String crawlDescription(Document document) {
      String description = "";

      Element descElement = document.select(".prod-desc .productDescription").first();
      if (descElement != null) {
         description = description + descElement.html();
      }

      return description;
   }

   private Prices crawlPrices(Float price) {
      Prices prices = new Prices();

      if (price != null) {
         Map<Integer, Float> mapInstallments = new HashMap<>();
         mapInstallments.put(1, price);

         prices.insertCardInstallment(Card.VISA.toString(), mapInstallments);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), mapInstallments);
         prices.insertCardInstallment(Card.DINERS.toString(), mapInstallments);
         prices.insertCardInstallment(Card.SHOP_CARD.toString(), mapInstallments);
         prices.insertCardInstallment(Card.NARANJA.toString(), mapInstallments);
         prices.insertCardInstallment(Card.NATIVA.toString(), mapInstallments);
         prices.insertCardInstallment(Card.AMEX.toString(), mapInstallments);

      }

      return prices;
   }

   private JSONArray crawlSkuJsonArray(Document document) {
      Elements scriptTags = document.getElementsByTag("script");
      JSONObject skuJson = null;
      JSONArray skuJsonArray = null;

      for (Element tag : scriptTags) {
         for (DataNode node : tag.dataNodes()) {
            if (tag.html().trim().startsWith("var skuJson_0 = ")) {
               skuJson = new JSONObject(node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1]
                     + node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1].split(Pattern.quote("}]};"))[0]);
            }
         }
      }

      if (skuJson != null && skuJson.has("skus")) {
         skuJsonArray = skuJson.getJSONArray("skus");
      }

      if (skuJsonArray == null) {
         skuJsonArray = new JSONArray();
      }

      return skuJsonArray;
   }

   private RatingsReviews scrapRatingReviews(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      if (isProductPage(doc)) {
         ratingReviews.setDate(session.getDate());

         JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

         if (skuJson.has("productId")) {
            String internalPid = Integer.toString(skuJson.getInt("productId"));
            Document docRating = crawlApiRatings(session.getOriginalURL(), internalPid);
            Integer totalNumOfEvaluations = getTotalNumOfRatings(docRating);
            Double avgRating = getTotalAvgRating(docRating, totalNumOfEvaluations);
            AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(docRating, doc);

            ratingReviews.setTotalRating(totalNumOfEvaluations);
            ratingReviews.setAverageOverallRating(avgRating);
            ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
            ratingReviews.setAdvancedRatingReview(advancedRatingReview);
         }
      }
      return ratingReviews;
   }

   private Document crawlApiRatings(String url, String internalPid) {

      String[] tokens = url.split("/");
      String productLinkId = tokens[tokens.length - 2];
      String payload = "productId=" + internalPid + "&productLinkId=" + productLinkId;
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/x-www-form-urlencoded");
      headers.put("Accept-Language", "pt-BR,pt;q=0.8,en-US;q=0.6,en;q=0.4");
      Request request =
            RequestBuilder.create().setUrl("https://www.walmart.com.ar/userreview").setCookies(cookies).setHeaders(headers).setPayload(payload).build();
      return Jsoup.parse(this.dataFetcher.post(session, request).getBody());
   }

   private Double getTotalAvgRating(Document docRating, Integer totalRating) {

      Double avgRating = 0.0;
      Elements rating = docRating.select("ul.rating");

      if (totalRating != null) {
         Double total = 0.0;

         for (Element e : rating) {
            Element star = e.select("strong").first();
            Element totalStar = e.select("li >  span:last-child").first();
            if (totalStar != null) {
               String votes = totalStar.text().replaceAll("[^0-9]", "").trim();
               if (!votes.isEmpty()) {
                  Integer totalVotes = Integer.parseInt(votes);
                  if (star != null) {
                     if (star.hasClass("avaliacao50")) {
                        total += totalVotes * 5;
                     } else if (star.hasClass("avaliacao40")) {
                        total += totalVotes * 4;
                     } else if (star.hasClass("avaliacao30")) {
                        total += totalVotes * 3;
                     } else if (star.hasClass("avaliacao20")) {
                        total += totalVotes * 2;
                     } else if (star.hasClass("avaliacao10")) {
                        total += totalVotes * 1;
                     }
                  }
               }
            }
         }

         avgRating = MathUtils.normalizeTwoDecimalPlaces(total / totalRating);
      }

      return avgRating;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document docRating, Document doc) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = docRating.select(".rating li");

      for (Element review : reviews) {

         Element elementStarNumber = review.selectFirst("Strong"); // <strong class="rating-demonstrativo avaliacao50"></strong>

         if (elementStarNumber != null) {

            String stringStarNumber = elementStarNumber.attr("class"); // rating-demonstrativo avaliacao50
            String sN = stringStarNumber.replaceAll("[^0-9]", "").trim(); // String 50
            Integer numberOfStars = !sN.isEmpty() ? Integer.parseInt(sN) : 0; // Integer 50

            Element elementVoteNumber = review.selectFirst("li > span:last-child"); // <span> 1 Voto</span>

            if (elementVoteNumber != null) {

               String vN = elementVoteNumber.text().replaceAll("[^0-9]", "").trim(); // 1 our ""
               Integer numberOfVotes = !vN.isEmpty() ? Integer.parseInt(vN) : 0; // 1 our 0

               switch (numberOfStars) {
                  case 50:
                     star5 = numberOfVotes;
                     break;
                  case 44:
                     star4 = numberOfVotes;
                     break;
                  case 30:
                     star3 = numberOfVotes;
                     break;
                  case 20:
                     star2 = numberOfVotes;
                     break;
                  case 10:
                     star1 = numberOfVotes;
                     break;
                  default:
                     break;
               }
            }
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

   private Integer getTotalNumOfRatings(Document docRating) {
      Integer totalRating = null;
      Element totalRatingElement = docRating.select(".media em > span").first();

      if (totalRatingElement != null) {
         String totalText = totalRatingElement.ownText().replaceAll("[^0-9]", "").trim();

         if (!totalText.isEmpty()) {
            totalRating = Integer.parseInt(totalText);
         }
      }

      return totalRating;
   }



}
