package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.YourreviewsRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilFemalepetCrawler extends Crawler {
  
  private static final String HOME_PAGE = "https://www.femalepet.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "femalepet";
  private static final String YOURVIEWS_API_KEY = "14bf3f36-845e-4e67-8ccf-629f69289f81";

  public BrasilFemalepetCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies, dataFetcher);

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);
      
      String internalPid = vtexUtil.crawlInternalPid(skuJson);
      RatingsReviews ratingReviews = scrapRatingsReviews(skuJson, internalPid);

      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb li > a", true);
      String description = CrawlerUtils.scrapSimpleDescription(doc,
          Arrays.asList(".productDescription", "#caracteristicas"));

      // sku data in json
      JSONArray arraySkus =
          skuJson != null &&
              skuJson.has("skus") &&
              !skuJson.isNull("skus")
                  ? skuJson.getJSONArray("skus")
                  : new JSONArray();

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = vtexUtil.crawlInternalId(jsonSku);
        JSONObject apiJSON = vtexUtil.crawlApi(internalId);
        String name = vtexUtil.crawlName(jsonSku, skuJson, " ");
        String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
        String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
        Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, false);
        Marketplace marketplace = vtexUtil.assembleMarketplaceFromMap(marketplaceMap);
        boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);
        Prices prices = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER) ? marketplaceMap.get(MAIN_SELLER_NAME_LOWER) : new Prices();
        Float price = vtexUtil.crawlMainPagePrice(prices);
        Integer stock = vtexUtil.crawlStock(apiJSON);
        JSONArray eanArray = CrawlerUtils.scrapEanFromVTEX(doc);
        RatingsReviews ratingReviewsClone = ratingReviews.clone();

        String ean = i < eanArray.length() ? eanArray.getString(i) : null;

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
            .setStock(stock)
            .setMarketplace(marketplace)
            .setEans(eans)
            .setRatingReviews(ratingReviewsClone)
            .build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }
    return products;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst("#produto") != null;
  }
  
  private RatingsReviews scrapRatingsReviews(JSONObject json, String internalPid) {
    RatingsReviews ratingReviews = new RatingsReviews();
    ratingReviews.setDate(session.getDate());
    
    YourreviewsRatingCrawler yr =
        new YourreviewsRatingCrawler(session, cookies, logger, YOURVIEWS_API_KEY, this.dataFetcher);
    
    Document docRating = yr.crawlPageRatingsFromYourViews(internalPid, YOURVIEWS_API_KEY, this.dataFetcher);
    
    Integer totalNumOfEvaluations = getTotalNumOfRatings(docRating);
    Double avgRating = getTotalAvgRating(docRating);
    AdvancedRatingReview advancedRatingReview = yr.getTotalStarsFromEachValue(internalPid);
    
    ratingReviews.setTotalRating(totalNumOfEvaluations);
    ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
    ratingReviews.setAverageOverallRating(avgRating);
    ratingReviews.setAdvancedRatingReview(advancedRatingReview);
    
    return ratingReviews;
  }

  private Double getTotalAvgRating(Document docRating) {
    Double avgRating = null;
    Element rating = docRating.selectFirst("meta[itemprop=ratingValue]");

    if (rating != null) {
      avgRating = Double.parseDouble(rating.attr("content"));
    }

    return avgRating;
  }
  
  private Integer getTotalNumOfRatings(Document doc) {
    Integer totalRating = null;
    Element totalRatingElement = doc.select("strong[itemprop=ratingCount]").first();

    if (totalRatingElement != null) {
      String totalText = totalRatingElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!totalText.isEmpty()) {
        totalRating = Integer.parseInt(totalText);
      }
    }

    return totalRating;
  }
}
