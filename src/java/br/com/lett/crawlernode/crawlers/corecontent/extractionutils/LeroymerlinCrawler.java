package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
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
 * Date: 12/10/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class LeroymerlinCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.leroymerlin.com.br";

   public LeroymerlinCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
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

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "[data-product-code]", "data-product-code");;
         String internalPid = scrapInternalPid(doc);
         String name = crawlName(doc);
         Float price = crawlPrice(doc);
         Prices prices = crawlPrices(price, doc);
         boolean available = crawlAvailability(doc);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb-item:not(:first-child) a .name", false);
         List<String> images = crawlImages(doc);
         String primaryImage = images.isEmpty() ? null : images.get(0);
         String secondaryImages = crawlSecondaryImages(images);
         String description =
               CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".product-header .product-text-description > div:first-child:not(.customer-service)",
                     "[name=descricao-do-produto]", ".product-info-details"));
         Integer stock = null;

         RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
         ratingReviewsCollection.addRatingReviews(crawlRating(doc, internalId));
         RatingsReviews ratingReviews = ratingReviewsCollection.getRatingReviews(internalId);

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
               .setStock(stock)
               .setMarketplace(new Marketplace())
               .setRatingReviews(ratingReviews)
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private RatingsReviews crawlRating(Document doc, String internalId) {
      RatingsReviews ratingReviews = new RatingsReviews();
      JSONObject reviewSummary = new JSONObject();
      JSONObject primaryRating = new JSONObject();

      ratingReviews.setDate(session.getDate());

      String endpointRequest = assembleBazaarVoiceEndpointRequest(internalId, "caag5mZC6wgKSPPhld3GSUVaOqO46ZEpAemNYqZ38m7Yc");
      Request request = RequestBuilder.create().setUrl(endpointRequest).setCookies(cookies).build();
      JSONObject ratingReviewsEndpointResponse = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

      if (ratingReviewsEndpointResponse.has("reviewSummary")) {
         reviewSummary = ratingReviewsEndpointResponse.getJSONObject("reviewSummary");

         if (reviewSummary.has("primaryRating")) {
            primaryRating = reviewSummary.getJSONObject("primaryRating");
         }
      }
      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(getTotalRating(reviewSummary));
      ratingReviews.setAverageOverallRating(getAverageOverallRating(primaryRating));
      ratingReviews.setTotalWrittenReviews(getTotalRating(reviewSummary));

      return ratingReviews;
   }

   private Integer getTotalRating(JSONObject reviewSummary) {
      Integer total = 0;

      if (reviewSummary.has("numReviews") && reviewSummary.get("numReviews") instanceof Integer) {
         total = reviewSummary.getInt("numReviews");
      }

      return total;
   }

   private Double getAverageOverallRating(JSONObject primaryRating) {
      Double average = 0d;

      if (primaryRating.has("average") && primaryRating.get("average") instanceof Double) {
         average = primaryRating.getDouble("average");
      }

      return average;
   }

   // https://api.bazaarvoice.com/data/display/0.2alpha/product/summary?PassKey=caag5mZC6wgKSPPhld3GSUVaOqO46ZEpAemNYqZ38m7Yc&productid=88100915
   // &contentType=reviews,questions&reviewDistribution=primaryRating,recommended&rev=0&contentlocale=pt_BR

   private String assembleBazaarVoiceEndpointRequest(String skuInternalId, String bazaarVoiceEnpointPassKey) {
      StringBuilder request = new StringBuilder();

      request.append("https://api.bazaarvoice.com/data/display/0.2alpha/product/summary?");
      request.append("&Passkey=" + bazaarVoiceEnpointPassKey);
      request.append("&productid=" + skuInternalId);
      request.append("&contentType=reviews,questions");
      request.append("&reviewDistribution=primaryRating,recommended");
      request.append("&rev=0");
      request.append("&contentlocale=pt_BR");

      return request.toString();
   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".product-code").isEmpty();
   }

   private String scrapInternalPid(Document doc) {
      String internalPid = null;

      Element internalPidElement = doc.selectFirst(".product-code");
      if (internalPidElement != null) {
         String text = internalPidElement.text();

         if (text.contains(".")) {
            internalPid = CommonMethods.getLast(text.split("\\.")).trim();
         } else if (text.contains("digo")) {
            internalPid = CommonMethods.getLast(text.split("digo")).trim();
         } else {
            internalPid = text.trim();
         }

         internalPid = internalPid.split(",")[0];
      }

      return internalPid;
   }

   private String crawlName(Document document) {
      String name = null;
      Element nameElement = document.selectFirst(".product-title");

      if (nameElement != null) {
         name = nameElement.ownText().trim();
      }

      return name;
   }

   private Float crawlPrice(Document document) {
      Float price = null;

      Elements salePriceElement = document.select(".product-price-tag .to-price .price-integer, .product-price-tag .to-price .price-decimal");
      if (!salePriceElement.isEmpty()) {
         price = MathUtils.parseFloatWithComma(salePriceElement.text());
      }

      return price;
   }

   private List<String> crawlImages(Document doc) {
      List<String> images = new ArrayList<>();

      Element divImages = doc.selectFirst(".product-carousel .carousel[data-items]");
      if (divImages != null) {
         JSONArray imagesArray = new JSONArray(divImages.attr("data-items"));

         for (Object o : imagesArray) {
            JSONObject imageJson = (JSONObject) o;

            // in this case the image is a video thumbnail
            if (imageJson.has("youtubeId") && imageJson.get("youtubeId") instanceof String && !imageJson.getString("youtubeId").trim().isEmpty()) {
               continue;
            }

            if (imageJson.has("shouldLoadImageZoom") && imageJson.getBoolean("shouldLoadImageZoom") && imageJson.has("zoomUrl")
                  && !imageJson.get("zoomUrl").toString().trim().isEmpty()) {
               images.add(imageJson.get("zoomUrl").toString());
            } else if (imageJson.has("url") && !imageJson.get("url").toString().trim().isEmpty()) {
               images.add(imageJson.get("url").toString());
            } else if (imageJson.has("thumb") && !imageJson.get("thumb").toString().trim().isEmpty()) {
               images.add(imageJson.get("thumb").toString());
            }
         }
      }

      return images;
   }

   private String crawlSecondaryImages(List<String> images) {
      String secondaryImages = null;

      if (!images.isEmpty()) {
         JSONArray secondaryImagesArray = new JSONArray(images);
         secondaryImagesArray.remove(0);

         if (secondaryImagesArray.length() > 0) {
            secondaryImages = secondaryImagesArray.toString();
         }
      }

      return secondaryImages;
   }

   private boolean crawlAvailability(Document doc) {
      return !doc.select(".product-purchase-buttons .buy-button[data-button=ecommerce]:not(.disabled), "
            + ".product-purchase-buttons .buy-button[data-button=pickupInStore]:not([disabled])").isEmpty();
   }

   /**
    * In the time when this crawler was made, this market hasn't bank ticket informations
    * 
    * @param doc
    * @param price
    * @return
    */
   private Prices crawlPrices(Float price, Document doc) {
      Prices prices = new Prices();

      if (price != null) {
         Map<Integer, Float> installmentPriceMap = new TreeMap<>();
         installmentPriceMap.put(1, price);

         Element pricesElements = doc.selectFirst(".product-price-tag > div");
         if (pricesElements != null) {

            if (pricesElements.hasAttr("data-from-price-integers") && pricesElements.hasAttr("data-from-price-decimals")) {
               String integers = pricesElements.attr("data-from-price-integers").trim();
               String decimals = pricesElements.attr("data-from-price-decimals").trim();

               if (!integers.isEmpty()) {
                  StringBuilder str = new StringBuilder();
                  str.append(integers.replace(".", ""));

                  if (!decimals.isEmpty()) {
                     str.append(".");
                     str.append(decimals);
                  }

                  prices.setPriceFrom(Double.parseDouble(str.toString()));
               }
            }

            if (pricesElements.hasAttr("data-branded-installments-amount") && pricesElements.hasAttr("data-branded-installments-value")) {
               String installment = pricesElements.attr("data-branded-installments-amount").trim();
               String value = pricesElements.attr("data-branded-installments-value").trim();

               if (!installment.isEmpty() && !value.isEmpty()) {
                  installmentPriceMap.put(Integer.parseInt(installment), MathUtils.parseFloatWithComma(value));
               }
            }
         }

         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      }

      return prices;
   }

}
