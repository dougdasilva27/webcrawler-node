package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilPetnanetCrawler extends Crawler {
  
  public BrasilPetnanetCrawler(Session session) {
    super(session);
  }

  private static final String HOME_PAGE = "https://www.petnanet.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "pet na net ";

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies, dataFetcher);

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);
      String internalPid = vtexUtil.crawlInternalPid(skuJson);
      RatingsReviews ratingReviews = scrapRatingsReviews(internalPid);

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
  
  private RatingsReviews scrapRatingsReviews(String internalPid) {
    RatingsReviews ratingReviews = new RatingsReviews();
    ratingReviews.setDate(session.getDate());
    
    Document docRating = crawlApiRatings(session.getOriginalURL(), internalPid);
    Integer totalNumOfEvaluations = getTotalNumOfRatings(docRating);
    AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(docRating);
    Double avgRating = computeAvgRating(advancedRatingReview);
    
    ratingReviews.setTotalRating(totalNumOfEvaluations);
    ratingReviews.setAverageOverallRating(avgRating);
    ratingReviews.setAdvancedRatingReview(advancedRatingReview);
    ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
    
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
        RequestBuilder.create().setUrl(HOME_PAGE + "userreview").setCookies(cookies).setHeaders(headers).setPayload(payload).build();
    return Jsoup.parse(this.dataFetcher.post(session, request).getBody());
  }

  private Double computeAvgRating(AdvancedRatingReview advancedRatingReview) {
    
    int total5 = advancedRatingReview.getTotalStar5();
    int total4 = advancedRatingReview.getTotalStar4();
    int total3 = advancedRatingReview.getTotalStar3();
    int total2 = advancedRatingReview.getTotalStar2();
    int total1 = advancedRatingReview.getTotalStar1();
    
    int totalTotal = total5 + total4 + total3 + total2 + total1;
    
    if(totalTotal == 0) {
      return 0.0;
    }
    
    return (total5 * 5.0 + total4 * 4.0 + total3 * 3.0 + total2 * 2.0 + total1 * 1.0) / totalTotal;
  }

  private Integer getTotalNumOfRatings(Document docRating) {
    Integer totalRating = null;
    Element totalRatingElement = docRating.selectFirst(".media em > span");

    if (totalRatingElement != null) {
      String totalText = totalRatingElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!totalText.isEmpty()) {
        totalRating = Integer.parseInt(totalText);
      }
    }

    return totalRating;
  }
  
  private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
    Integer star1 = 0;
    Integer star2 = 0;
    Integer star3 = 0;
    Integer star4 = 0;
    Integer star5 = 0;
    
    Elements rating = doc.select("ul.rating li");
    
    for (Element e : rating) {
      Element star = e.selectFirst("strong.rating-demonstrativo");
      Element totalStar = e.selectFirst("> span:not([class])");

      if (totalStar != null) {
        String votes = totalStar.text().replaceAll("[^0-9]", "").trim();

        if (!votes.isEmpty()) {
          Integer totalVotes = Integer.parseInt(votes);
          
          if (star != null) {
            if (star.hasClass("avaliacao50")) {
              star5 = totalVotes;
            } else if (star.hasClass("avaliacao40")) {
              star4 = totalVotes;
            } else if (star.hasClass("avaliacao30")) {
              star3 = totalVotes;
            } else if (star.hasClass("avaliacao20")) {
              star2 = totalVotes;
            } else if (star.hasClass("avaliacao10")) {
              star1 = totalVotes;
            }
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
}
