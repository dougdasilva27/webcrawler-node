package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONObject;
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
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

/**
 * Date: 28/11/2016
 * 
 * 1) Only one sku per page.
 * 
 * Price crawling notes: 1) In time crawler was made, there no product unnavailable. 2) There is no
 * bank slip (boleto bancario) payment option. 3) There is no installments for card payment. So we
 * only have 1x payment, and to this value we use the cash price crawled from the sku page. (nao
 * existe divisao no cartao de credito).
 * 
 * @author Gabriel Dornelas
 *
 */
public class MexicoSuperamaCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.superama.com.mx/";

   public MexicoSuperamaCrawler(Session session) {
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

         String internalId = crawlInternalId(doc);
         String name = crawlName(doc);
         Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".btnAddToCarDetail[data-precio]", "data-precio", true, '.', session);
         Prices prices = crawlPrices(price);
         boolean available = crawlAvailability(doc);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "#breadcrumbNav ul li:not(.mobile):not(.hidden-sm):not(:first-child)");
         String primaryImage =
               CrawlerUtils.scrapSimplePrimaryImage(doc, ".pdp-preview-image a", Arrays.asList("href", "id"), "https:", "www.superama.com.mx");
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".pdp-preview-image a", Arrays.asList("href", "id"), "https:",
               "www.superama.com.mx", primaryImage);
         String description = crawlDescription(doc);
         Integer stock = null;
         RatingsReviews ratingReviews = crawlRatingReviews(internalId);
         List<String> eans = scrapEans(doc);
         // Creating the product
         Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setName(name).setPrice(price)
               .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
               .setStock(stock).setMarketplace(new Marketplace()).setRatingReviews(ratingReviews).setEans(eans).build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private List<String> scrapEans(Document doc) {
      List<String> eans = new ArrayList<>();
      Element upc = doc.selectFirst(".container #upcProducto");

      if (upc != null) {
         eans.add(upc.attr("value"));
      }
      return eans;
   }

   private RatingsReviews crawlRatingReviews(String internalId) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      String bazaarVoicePassKey = "ca00NLtrMkSnTddOCbktCnskwSV7OaQHCOTa3EZNMR2KE";
      String endpointRequest = assembleBazaarVoiceEndpointRequest(internalId, bazaarVoicePassKey, 0, 5);

      Request request = RequestBuilder.create().setUrl(endpointRequest).setCookies(cookies).build();
      JSONObject ratingReviewsEndpointResponse = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
      JSONObject reviewStatistics = getReviewStatisticsJSON(ratingReviewsEndpointResponse, internalId);

      ratingReviews.setTotalRating(getTotalReviewCount(reviewStatistics));
      ratingReviews.setAverageOverallRating(getAverageOverallRating(reviewStatistics));

      return ratingReviews;
   }

   private Integer getTotalReviewCount(JSONObject reviewStatistics) {
      Integer totalReviewCount = 0;
      if (reviewStatistics.has("TotalReviewCount")) {
         totalReviewCount = reviewStatistics.getInt("TotalReviewCount");
      }
      return totalReviewCount;
   }

   private Double getAverageOverallRating(JSONObject reviewStatistics) {
      Double avgOverallRating = 0d;
      if (reviewStatistics.has("AverageOverallRating")) {
         avgOverallRating = reviewStatistics.getDouble("AverageOverallRating");
      }
      return avgOverallRating;
   }

   private String assembleBazaarVoiceEndpointRequest(String skuInternalPid, String bazaarVoiceEnpointPassKey, Integer offset, Integer limit) {

      StringBuilder request = new StringBuilder();

      request.append("http://api.bazaarvoice.com/data/reviews.json?apiversion=5.4");
      request.append("&passkey=" + bazaarVoiceEnpointPassKey);
      request.append("&Offset=" + offset);
      request.append("&Limit=" + limit);
      request.append("&Sort=SubmissionTime:desc");
      request.append("&Filter=ProductId:" + skuInternalPid);
      request.append("&Include=Products");
      request.append("&Stats=Reviews");

      return request.toString();
   }


   private JSONObject getReviewStatisticsJSON(JSONObject ratingReviewsEndpointResponse, String internalId) {
      if (ratingReviewsEndpointResponse.has("Includes")) {
         JSONObject includes = ratingReviewsEndpointResponse.getJSONObject("Includes");

         if (includes.has("Products")) {
            JSONObject products = includes.getJSONObject("Products");

            if (products.has(internalId)) {
               JSONObject product = products.getJSONObject(internalId);

               if (product.has("ReviewStatistics")) {
                  return product.getJSONObject("ReviewStatistics");
               }
            }
         }
      }

      return new JSONObject();
   }

   private boolean isProductPage(Document doc) {
      return !doc.select("#upcProducto").isEmpty();
   }

   private String crawlInternalId(Document document) {
      String internalId = null;

      Element internalIdElement = document.select("#upcProducto").first();
      if (internalIdElement != null) {
         internalId = internalIdElement.attr("value");
      }

      return internalId;
   }

   private String crawlName(Document document) {
      String name = null;
      Element nameElement = document.select("#nombreProductoDetalle").first();

      if (nameElement != null) {
         name = nameElement.text().trim();
      }

      return name;
   }

   private boolean crawlAvailability(Document document) {
      boolean available = false;

      Element outOfStockElement = document.select(".btnAddToCarDetail").first();
      if (outOfStockElement != null) {
         available = true;
      }

      return available;
   }

   private String crawlDescription(Document document) {
      StringBuilder description = new StringBuilder();

      if (document.select(".moreinfo__ingredientes").first() != null) {
         description.append(" Ingredientes ");
      }

      if (document.select(".moreinfo__nutricional").first() != null) {
         description.append(" Información Nutricional ");
      }

      if (document.select(".moreinfo__caracteristicas").first() != null) {
         description.append(" Características ");
      }

      description.append(CrawlerUtils.scrapSimpleDescription(document, Arrays.asList(".detail-description-content", "#tab_descripcion",
            ".content__caracteristicas", ".content__ingredientes", "#ContenedorInfoNutriId")));

      return description.toString();
   }

   /**
    * There is no bankSlip price.
    * 
    * There is no card payment options, other than cash price. So for installments, we will have only
    * one installment for each card brand, and it will be equals to the price crawled on the sku main
    * page.
    * 
    * @param doc
    * @param price
    * @return
    */
   private Prices crawlPrices(Float price) {
      Prices prices = new Prices();

      if (price != null) {
         Map<Integer, Float> installmentPriceMap = new TreeMap<Integer, Float>();
         installmentPriceMap.put(1, price);

         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      }

      return prices;
   }

}
