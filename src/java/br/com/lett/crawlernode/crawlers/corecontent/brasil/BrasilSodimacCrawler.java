package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilSodimacCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.sodimac.com.br/sodimac-br/";
   public static final String PRODUCT_API = "https://www.sodimac.com.br/sodimac-br/productDetail/ajax/switchSKU.jsp";

   public BrasilSodimacCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String internalPid = crawlInternalPid(doc);
         String[] skuIDs = getJSONArray(doc);
         String description = crawlDescription(doc);
         CategoryCollection categories = extractCategories(doc);
         if (skuIDs != null) {
            for (String internalId : skuIDs) {
               Document fetchedData = fetchAPIProduct(internalId);
               String name = crawlName(fetchedData);
               Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".t-black.bold.price, .mt2-price", null, false, ',', session);
               Prices prices = crawlPrices(price);

               boolean available = crawlAvailability(fetchedData) && price != null;
               List<String> imagesArr = getImagesFromAPI(internalId);
               String primaryImage = crawlPrimaryImage(imagesArr);
               String secondaryImages = crawlSecondaryImages(imagesArr);
               RatingsReviews ratingReviews = crawlRating(internalId);
               Product product = ProductBuilder.create()
                     .setUrl(session.getOriginalURL())
                     .setInternalId(internalId)
                     .setInternalPid(internalPid)
                     .setName(name)
                     .setPrice(price)
                     .setPrices(prices)
                     .setAvailable(available)
                     .setPrimaryImage(primaryImage)
                     .setSecondaryImages(secondaryImages)
                     .setDescription(description)
                     .setCategory1(categories.getCategory(0))
                     .setCategory2(categories.getCategory(1))
                     .setCategory3(categories.getCategory(2))
                     .setMarketplace(new Marketplace())
                     .setRatingReviews(ratingReviews).build();
               products.add(product);
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   private Document fetchAPIProduct(String id) {
      Document fetchedData = new Document("");
      if (id != null) {
         String payload = "?type=html&relatedProductcolorDimension=false&relatedProductsizeDimension=false&productId=" + id;
         Request request = RequestBuilder.create().setUrl(PRODUCT_API + payload).setCookies(cookies).build();
         fetchedData = Jsoup.parse(this.dataFetcher.get(session, request).getBody());
      }
      return fetchedData;
   }

   private CategoryCollection extractCategories(Document doc) {
      CategoryCollection categories = new CategoryCollection();
      doc.select("span[itemprop=\"itemListElement\"]:not(:first-child):not(:last-child) > a > span").forEach(elem -> categories.add(elem.text().trim()));
      return categories;
   }

   private String requestRatingStr(String passkey, String sku) {
      StringBuilder requestString = new StringBuilder();
      requestString.append("https://api.bazaarvoice.com/data/batch.json?apiversion=5.5")
            .append("&passkey=" + passkey)
            .append("&displaycode=13566-pt_br")
            .append("&resource.q0=products")
            .append("&filter.q0=id%3Aeq%3A" + sku)
            .append("&stats.q0=reviews")
            .append("&filteredstats.q0=reviews")
            .append("&filter_reviews.q0=contentlocale%3Aeq%3Apt_BR")
            .append("&filter_reviewcomments.q0=contentlocale%3Aeq%3Apt_BR")
            .append("&resource.q1=reviews")
            .append("&filter.q1=isratingsonly%3Aeq%3Afalse")
            .append("&filter.q1=productid%3Aeq%3A" + sku)
            .append("&filter.q1=contentlocale%3Aeq%3Apt_BR")
            .append("&sort.q1=relevancy%3Aa1")
            .append("&stats.q1=reviews")
            .append("&filteredstats.q1=reviews")
            .append("&include.q1=authors%2Cproducts%2Ccomments")
            .append("&filter_reviews.q1=contentlocale%3Aeq%3Apt_BR")
            .append("&filter_reviewcomments.q1=contentlocale%3Aeq%3Apt_BR")
            .append("&filter_comments.q1=contentlocale%3Aeq%3Apt_BR")
            .append("&limit.q1=8")
            .append("&offset.q1=0")
            .append("&limit_comments.q1=3")
            .append("&callback=BV._internal.dataHandler0");
      return requestString.toString();
   }

   private RatingsReviews crawlRating(String sku) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());
      final String passKey = "caqjqAaqxCYDSdSy6LKE5VYUt0QtY88namGrRtuTkSSvU";
      String endpointRequest = requestRatingStr(passKey, sku);
      JSONObject ratingReviewsEndpoint = sendRequestRating(endpointRequest);
      JSONObject jsonRatingInc = ratingReviewsEndpoint.getJSONObject("BatchedResults").getJSONObject("q1").getJSONObject("Includes");
      Integer total = 0;
      Double overallRating = 0D;
      if (jsonRatingInc.has("Products")) {
         JSONObject jsonProduct = jsonRatingInc.getJSONObject("Products").getJSONObject(sku).getJSONObject("FilteredReviewStatistics");
         total = getTotalReviewCount(jsonProduct);
         overallRating = getAverageOverallRating(jsonProduct);
         AdvancedRatingReview advancedRatingReview = extractAdvancedRating(jsonProduct);
         ratingReviews.setAdvancedRatingReview(advancedRatingReview);
      }
      ratingReviews.setTotalRating(total);
      ratingReviews.setTotalWrittenReviews(total);
      ratingReviews.setAverageOverallRating(overallRating);
      return ratingReviews;
   }

   private AdvancedRatingReview extractAdvancedRating(JSONObject jsonProduct) {
      AdvancedRatingReview advancedRating = new AdvancedRatingReview();

      for (Object obj : jsonProduct.getJSONArray("RatingDistribution")) {
         JSONObject jsonRating = (JSONObject) obj;
         int int1 = jsonRating.getInt("RatingValue");
         if (int1 == 1) {
            advancedRating.setTotalStar1(jsonRating.getInt("Count"));
         } else if (int1 == 2) {
            advancedRating.setTotalStar2(jsonRating.getInt("Count"));
         } else if (int1 == 3) {
            advancedRating.setTotalStar3(jsonRating.getInt("Count"));
         } else if (int1 == 4) {
            advancedRating.setTotalStar4(jsonRating.getInt("Count"));
         } else if (int1 == 5) {
            advancedRating.setTotalStar5(jsonRating.getInt("Count"));
         }
      }
      return advancedRating;
   }

   /**
    * 
    * @param endpointRequest URL a ser enviada a requisição para a API de rating review
    * @return rating reviews json
    */
   private JSONObject sendRequestRating(String endpointRequest) {
      Request request = RequestBuilder.create().setUrl(endpointRequest).setCookies(cookies).build();
      String ratingReviewsEndpointResponseStr = CrawlerUtils.extractSpecificStringFromScript(this.dataFetcher.get(session, request).getBody(),
            "dataHandler0(", false, "})", true);
      return JSONUtils.stringToJson(ratingReviewsEndpointResponseStr);
   }

   private Double getAverageOverallRating(JSONObject reviewStatistics) {
      return reviewStatistics.getDouble("AverageOverallRating");
   }

   private Integer getTotalReviewCount(JSONObject ratingReviewsEndpointResponse) {
      return ratingReviewsEndpointResponse.getInt("TotalReviewCount");
   }

   private String crawlDescription(Document doc) {
      StringBuilder description = new StringBuilder();
      Element descriptionHeader = doc.selectFirst("section.prod-car > header > h3 > span");
      if (descriptionHeader != null) {
         description.append(descriptionHeader.outerHtml());
      }
      Element descriptionBody = doc.selectFirst(".row.prod-desc > div");
      if (descriptionBody != null) {
         description.append(descriptionBody.outerHtml());
      }
      Element descriptionTecnic = doc.selectFirst("section.car-body > table.prod-ficha");
      if (descriptionTecnic != null) {
         description.append(descriptionTecnic.outerHtml());
      }
      return description.toString();
   }

   /**
    * 
    * @param doc
    * @return json with all product content
    */
   private String[] getJSONArray(Document doc) {
      Element infoDocs = doc.selectFirst("#JsonArray");
      String[] idArray = null;
      if (infoDocs != null) {
         JSONArray skuObjArr = new JSONArray(infoDocs.text());
         JSONObject skuObject = skuObjArr.getJSONObject(0);
         if (skuObject.has("pickupInStore")) {
            String valueId = skuObject.getString("pickupInStore");
            valueId = valueId.substring(1, valueId.length() - 1).replace("=true", "").replace("=false", "").replace(" ", "");
            idArray = valueId.split(",");
            return idArray;
         }
      } else {
         JSONObject skuObj = CrawlerUtils.selectJsonFromHtml(doc, ".pdp script[language=\"JavaScript\"]", "var digitalData =", "};", false, false);
         if (skuObj.has("ecom") && !skuObj.isNull("ecom")) {
            idArray = new String[1];
            JSONObject ecom = skuObj.getJSONObject("ecom");
            if (ecom.has("sku") && !ecom.isNull("sku")) {
               idArray[0] = ecom.get("sku").toString();
            }
         }
      }
      return idArray;

   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("#productTitleDisplayContainer") != null;
   }

   private String crawlInternalPid(Document doc) {
      String internalId = null;
      Element id = doc.selectFirst("#currentSkuId");
      if (id != null) {
         internalId = id.attr("value");
      }
      return internalId;
   }

   private String crawlName(Document doc) {
      String name = null;
      Element nameElement = doc.selectFirst("#productTitleDisplayContainer");
      if (nameElement != null) {
         name = nameElement.text();
      }
      return name;
   }

   private Prices crawlPrices(Float price) {
      Prices prices = new Prices();
      if (price != null) {
         Map<Integer, Float> installmentPriceMap = new TreeMap<>();
         installmentPriceMap.put(1, price);
         prices.setPriceFrom(Double.parseDouble(price.toString()));
         prices.setBankTicketPrice(price);
         prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      }
      return prices;
   }


   private boolean crawlAvailability(Document doc) {
      return doc.select(".btn-addToCart").first() != null;
   }

   private List<String> getImagesFromAPI(String id) {
      List<String> imagesArr = new ArrayList<>();
      String productAPI = "http://sodimac.scene7.com/is/image/SodimacBrasil/" + id + "?req=set,json";
      String imagesFetched = null;
      if (id != null) {
         Request request = RequestBuilder.create().setUrl(productAPI).setCookies(cookies).build();
         imagesFetched = this.dataFetcher.get(session, request).getBody();
         imagesFetched = imagesFetched.substring(31, imagesFetched.length() - 6);
         JSONObject imagesObj = new JSONObject(imagesFetched);
         String imageURL = null;
         Object obj = imagesObj.get("item");
         if (obj instanceof JSONArray) {
            JSONArray imagesObjArray = (JSONArray) obj;
            for (int i = 0; i < imagesObjArray.length(); i++) {
               imageURL = "http://sodimac.scene7.com/is/image/" + imagesObjArray.getJSONObject(i).getJSONObject("s").get("n").toString();
               imagesArr.add(imageURL);
            }
         } else if (obj instanceof JSONObject) {
            imagesObj = imagesObj.getJSONObject("item");
            imageURL = "http://sodimac.scene7.com/is/image/" + imagesObj.getJSONObject("s").get("n").toString();
            imagesArr.add(imageURL);
         }
      }
      return imagesArr;
   }

   private String crawlPrimaryImage(List<String> images) {
      String primaryImage = null;
      if (!images.isEmpty()) {
         primaryImage = images.get(0);
      }
      return primaryImage;
   }

   private String crawlSecondaryImages(List<String> images) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();
      if (!images.isEmpty()) {
         for (int i = 1; i < images.size(); i++) {
            secondaryImagesArray.put(images.get(i));
         }
         secondaryImages = secondaryImagesArray.toString();
      }
      return secondaryImages;
   }
}
