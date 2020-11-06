package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
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
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

/**
 * Date: 26/03/2019
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilBuscapeCrawler extends Crawler {

   private static final String HOST = "www.buscape.com.br";
   private static final String PROTOCOL = "https";
   private static final String HOME_PAGE = "https://www.buscape.com.br/";

   public BrasilBuscapeCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {

      if (!doc.select(".prod-page").isEmpty()) {
         return scrapV1(doc);
      } else if (doc.selectFirst("#__NEXT_DATA__") != null) {
         return scrapV2(doc);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
         return new ArrayList<>();
      }
   }

   private List<Product> scrapV1(Document doc) throws MalformedProductException {
      List<Product> products = new ArrayList<>();

      JSONObject productJson = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"application/ld+json\"]", null, null, false, false);

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=prodid]", "value");
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name", false);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".zbreadcrumb li:not(:first-child) a");
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".tech-spec-table"));
      String primaryImage = scrapPrimaryImageV1(doc);
      String secondaryImages = scrapSecondaryImagesV1(doc, primaryImage);
      List<Document> offersHtmls = getSellersHtmlsV1(doc, internalId);
      Marketplace marketplace = scrapMarketplacesV1(offersHtmls);
      RatingsReviews ratingAndReviews = scrapRatingAndReviewsV1(internalId, productJson);
      // Creating the product
      Product product = ProductBuilder.create()
         .setUrl(session.getOriginalURL())
         .setInternalId(internalId)
         .setName(name)
         .setPrices(new Prices())
         .setAvailable(false)
         .setCategory1(categories.getCategory(0))
         .setCategory2(categories.getCategory(1))
         .setCategory3(categories.getCategory(2))
         .setPrimaryImage(primaryImage)
         .setRatingReviews(ratingAndReviews)
         .setSecondaryImages(secondaryImages)
         .setDescription(description)
         .setMarketplace(marketplace)
         .build();

      products.add(product);

      return products;
   }

   private List<Product> scrapV2(Document doc) throws MalformedProductException {
      List<Product> products = new ArrayList<>();

      JSONObject dataJson = CrawlerUtils.selectJsonFromHtml(doc, "#__NEXT_DATA__", null, null, false, false);

      JSONObject productGeneral = JSONUtils.getValueRecursive(dataJson, "props.pageProps.page.product", JSONObject.class);

      if (productGeneral == null) {
         return products;
      }

      List<String> images = scrapImages(productGeneral);

      JSONObject productJson = JSONUtils.getJSONValue(productGeneral, "product");

      String internalId = productJson.optString("id");
      String name = productJson.optString("name");

      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".Breadcrumb_List__1RAzt li:not(:first-child) span");
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".ProductPageBody_Col__23zGY .ProductPageBody_DetailsSection___68li"));

      String primaryImage = null;

      if (images.size() > 0) {
         primaryImage = images.remove(0);
      }

      Marketplace marketplace = scrapMarketplacesV2(internalId, productJson);
      RatingsReviews ratingAndReviews = scrapRatingAndReviewsV2(internalId, productGeneral);

      Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setName(name)
            .setPrices(new Prices())
            .setAvailable(false)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setRatingReviews(ratingAndReviews)
            .setDescription(description)
            .setMarketplace(marketplace)
            .build();

      products.add(product);

      return products;
   }

   private List<String> scrapImages(JSONObject productGeneral) {
      List<String> images = new ArrayList<>();

      JSONArray mediaImages = JSONUtils.getJSONArrayValue(productGeneral, "mediaImages");

      for (Object mediaImageObj: mediaImages) {
         if (mediaImageObj instanceof JSONObject) {
            JSONObject mediaImage = (JSONObject) mediaImageObj;
            if (mediaImage.optString("type").equals("PRODUCT_ZOOM")) {
               String image = mediaImage.optString("url");
               if (!image.isEmpty()){
                  images.add(image);
               }
            }
         }
      }

      return images;
   }

   private List<Document> getSellersHtmlsV1(Document doc, String internalId) {
      List<Document> htmls = new ArrayList<>();
      htmls.add(doc);

      Integer offersNumber = CrawlerUtils.scrapIntegerFromHtmlAttr(doc, ".offers-list[data-showing-total]", "data-showing-total", 0);
      int offersNumberCount = doc.select(".offers-list__offer").size();

      if (offersNumber > offersNumberCount) {
         int currentPage = 1;

         while (offersNumber > offersNumberCount) {
            String offerUrl = "https://www.buscape.com.br/ajax/product_desk?__pAct_=getmoreoffers"
                  + "&prodid=" + internalId
                  + "&pagenumber=" + currentPage
                  + "&highlightedItemID=0"
                  + "&resultorder=7";

            Request request = RequestBuilder.create()
                  .setUrl(offerUrl)
                  .setCookies(cookies)
                  .build();

            Document offersDoc = Jsoup.parse(this.dataFetcher.get(session, request).getBody());
            Elements offersElements = offersDoc.select(".offers-list__offer");
            if (!offersElements.isEmpty()) {
               offersNumberCount += offersElements.size();
               htmls.add(offersDoc);
            } else {
               break;
            }

            currentPage++;
         }
      }

      return htmls;
   }

   private Marketplace scrapMarketplacesV1(List<Document> docuements) {
      Map<String, Prices> offersMap = new HashMap<>();

      for (Document doc : docuements) {
         Elements offers = doc.select(".offers-list__offer");
         for (Element e : offers) {
            String sellerName = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".col-store a", "title");

            if (sellerName != null) {
               String sellerNameLower = sellerName.replace("na", "").trim().toLowerCase();
               Prices prices = scrapPricesOnNewSite(doc);

               offersMap.put(sellerNameLower, prices);
            }
         }
      }

      return CrawlerUtils.assembleMarketplaceFromMap(offersMap, Arrays.asList("buscapé"), Card.VISA, session);
   }

   private Marketplace scrapMarketplacesV2(String internalId, JSONObject productJson) {
      Map<String, Prices> offersMap = new HashMap<>();

      int stores = productJson.optInt("stores");

      String url = "https://api-v1.zoom.com.br/esfiha/product/" + internalId + "?order=DEFAULT&page=1&pageSize=100";

      Request request = RequestBuilder.create()
            .setUrl(url)
            .setCookies(cookies)
            .build();

      JSONObject jsonStore = JSONUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

      JSONArray hits = JSONUtils.getJSONArrayValue(jsonStore, "hits");

      for (Object hit : hits) {
         if (hit instanceof JSONObject) {
            String sellerName = JSONUtils.getValueRecursive(hit, "seller.name", String.class);
            if (sellerName != null) {
               Prices prices = scrapPricesV2(JSONUtils.getJSONValue((JSONObject) hit, "sales_condition"));
               offersMap.put(sellerName.toLowerCase(), prices);
            }

         }
      }

      return CrawlerUtils.assembleMarketplaceFromMap(offersMap, Arrays.asList("buscapé"), Card.VISA, session);
   }

   private Prices scrapPricesOnNewSite(Element doc) {
      Prices prices = new Prices();

      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".price__total", null, true, ',', session);
      if (price != null) {
         Map<Integer, Float> installmentPriceMap = new TreeMap<>();
         installmentPriceMap.put(1, price);
         prices.setBankTicketPrice(price);

         Pair<Integer, Float> installments = CrawlerUtils.crawlSimpleInstallment(".price__parceled__parcels", doc, false);
         if (!installments.isAnyValueNull()) {
            installmentPriceMap.put(installments.getFirst(), installments.getSecond());
         }

         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      }

      return prices;
   }

   private Prices scrapPricesV2(JSONObject priceJson) {

      Prices prices = new Prices();

      Float price = priceJson.optFloat("price");

      if (price > 0) {
         Map<Integer, Float> installmentPriceMap = new TreeMap<>();
         installmentPriceMap.put(1, price);
         prices.setBankTicketPrice(price);

         JSONArray installmentsArray = JSONUtils.getJSONArrayValue(priceJson, "installments");
         for (Object installmentJSON : installmentsArray) {
            if (installmentJSON instanceof JSONObject) {
               int installment = ((JSONObject) installmentJSON).optInt("amount_months");
               if (installment > 1) {
                  float installmentPrice = ((JSONObject) installmentJSON).optFloat("price", 0);
                  if (installmentPrice > 0) {
                     installmentPriceMap.put(installment, installmentPrice);
                  }
               }
            }
         }

         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      }

      return prices;
   }

   private String scrapPrimaryImageV1(Document doc) {
      JSONArray jsonImg = getImageJSON(doc);
      String imagem = null;
      if (jsonImg != null) {
         JSONObject imageJson = jsonImg.getJSONObject(0);
         imagem = getImageFromJSONOfNewSite(imageJson);
      }
      return imagem;
   }

   private String scrapSecondaryImagesV1(Document doc, String primaryImage) {
      JSONArray secondaryImagesArray = new JSONArray();
      JSONArray jsonImg = getImageJSON(doc);

      for (Object object : jsonImg) {
         String image = getImageFromJSONOfNewSite((JSONObject) object);
         if (!image.equalsIgnoreCase(primaryImage)) {
            secondaryImagesArray.put(image);
         }
      }

      return secondaryImagesArray.toString();
   }

   private JSONArray getImageJSON(Document doc) {
      Elements scripts = doc.select("body > script");
      JSONArray jsonImg = null;
      JSONObject jsonProduct = null;
      for (Element element : scripts) {
         if (element.toString().contains("ProductUrl:\"" + session.getOriginalURL() + '"')) {
            String jsonStringProduct = CrawlerUtils.extractSpecificStringFromScript(element.toString(), "Zoom.Context.load(", true, "});", false);
            jsonProduct = JSONUtils.stringToJson(jsonStringProduct);
            break;
         }
      }
      if (jsonProduct != null) {
         JSONObject jsonSuperZoom = jsonProduct.optJSONObject("SuperZoom");
         if (jsonSuperZoom != null) {
            jsonImg = jsonSuperZoom.optJSONArray("img");
         }
      }
      return jsonImg;
   }

   private String getImageFromJSONOfNewSite(JSONObject imageJson) {
      String image = null;

      if (imageJson.opt("l") instanceof String) {
         image = CrawlerUtils.completeUrl(imageJson.get("l").toString().trim(), PROTOCOL, HOST);
      } else if (imageJson.opt("m") instanceof String) {
         image = CrawlerUtils.completeUrl(imageJson.get("m").toString().trim(), PROTOCOL, HOST);
      } else if (imageJson.opt("t") instanceof String) {
         image = CrawlerUtils.completeUrl(imageJson.get("t").toString().trim(), PROTOCOL, HOST);
      }

      return image;
   }

   private RatingsReviews scrapRatingAndReviewsV1(String internalId, JSONObject productJson) {
      RatingsReviews ratingReviews = new RatingsReviews();

      ratingReviews.setDate(session.getDate());
      ratingReviews.setInternalId(internalId);

      JSONObject ratingJson = productJson.optJSONObject("aggregateRating");
      if (ratingJson instanceof JSONObject) {
         Integer totalNumOfEvaluations = CrawlerUtils.getIntegerValueFromJSON(ratingJson, "reviewCount", 0);
         Double avgRating = CrawlerUtils.getDoubleValueFromJSON(ratingJson, "ratingValue", true, false);
         ratingReviews.setTotalRating(totalNumOfEvaluations);
         ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
         ratingReviews.setAdvancedRatingReview(getAdvancedRating(internalId, totalNumOfEvaluations));
         ratingReviews.setAverageOverallRating(avgRating != null ? avgRating : 0D);
      }
      return ratingReviews;
   }

   private RatingsReviews scrapRatingAndReviewsV2(String internalId, JSONObject productJson) {
      RatingsReviews ratingReviews = new RatingsReviews();

      ratingReviews.setDate(session.getDate());
      ratingReviews.setInternalId(internalId);

      Integer totalNumOfEvaluations = productJson.optInt("countOfComments");
      Double avgRating = JSONUtils.getValueRecursive(productJson, "product.rating", Double.class);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating != null ? avgRating : 0D);
      ratingReviews.setAdvancedRatingReview(getAdvancedRatingV2(productJson));

      return ratingReviews;
   }

   private AdvancedRatingReview getAdvancedRatingV2(JSONObject productJson) {
      AdvancedRatingReview advancedRating = CrawlerUtils.advancedRatingEmpty();

      JSONArray comments = JSONUtils.getJSONArrayValue(productJson, "comments");

      for (Object comment : comments) {
         if (comment instanceof JSONObject) {
            Integer star = ((JSONObject) comment).optInt("rating");
            CrawlerUtils.incrementAdvancedRating(advancedRating, star);
         }
      }

      return advancedRating;
   }

   private AdvancedRatingReview getAdvancedRating(String internalId, int total) {
      int pageNumber = 0;
      int totalProds = 0;
      AdvancedRatingReview advancedRating = CrawlerUtils.advancedRatingEmpty();

      if (total == 0) {
         return advancedRating;
      }

      String response = null;
      Document document = null;
      do {
         String url = new StringBuilder()
               .append(session.getOriginalURL()).append("?")
               .append("__pAct_=getmoreprodcomments")
               .append("&productid=").append(internalId)
               .append("&commentsorder=RELEVANCE")
               .append("&&pagenumber=").append(pageNumber).toString();

         Request request = RequestBuilder.create()
               .setUrl(url)
               .setCookies(cookies)
               .build();
         response = this.dataFetcher.get(session, request).getBody();

         if (!response.isEmpty()) {
            document = Jsoup.parse(response);

            Elements ratingDoc = document.select(".product-rating :first-child");
            if (!ratingDoc.isEmpty()) {
               for (Element element : ratingDoc) {
                  Integer star = MathUtils.parseInt(element.className());
                  if (star != null) {
                     totalProds++;
                     CrawlerUtils.incrementAdvancedRating(advancedRating, star);
                  }
               }
            } else {
               JSONObject dataJson = CrawlerUtils.selectJsonFromHtml(document, "#__NEXT_DATA__", null, null, false, false);
               JSONObject productJson = JSONUtils.getValueRecursive(dataJson, "props.pageProps.page.product", JSONObject.class);
               if (productJson != null) {
                  return getAdvancedRatingV2(productJson);
               }
            }
         }
         pageNumber++;
      } while (totalProds < total && !response.isEmpty());
      return advancedRating;
   }

}
