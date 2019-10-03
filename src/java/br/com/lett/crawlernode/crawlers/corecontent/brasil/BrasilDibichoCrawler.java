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
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilDibichoCrawler extends Crawler {

  public BrasilDibichoCrawler(Session session) {
    super(session);
  }

  private static final String HOME_PAGE = "https://www.dibicho.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "loja di bicho";

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
        RatingsReviews ratingReviewsClone = (RatingsReviews) ratingReviews.clone();

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
    return doc.selectFirst(".mainProduct") != null;
  }
  
  private RatingsReviews scrapRatingsReviews(String internalPid) {
    RatingsReviews ratingReviews = new RatingsReviews();
    ratingReviews.setDate(session.getDate());
    
    Document docRating = crawlApiRatings(session.getOriginalURL(), internalPid);
    Integer totalNumOfEvaluations = getTotalNumOfRatings(docRating);
    Double avgRating = getTotalAvgRating(docRating, totalNumOfEvaluations);
    
    ratingReviews.setTotalRating(totalNumOfEvaluations);
    ratingReviews.setAverageOverallRating(avgRating);
    
    return ratingReviews;
  }
  
  /**
   * Api Ratings Url: http://www.walmart.com.ar/userreview Ex payload:
   * productId=8213&productLinkId=home-theater-5-1-microlab-m-710u51 Required headers to crawl this
   * api
   * 
   * @param url
   * @param internalPid
   * @return document
   */
  private Document crawlApiRatings(String url, String internalPid) {
    
    // Parameter in url for request POST ex: "led-32-ilo-hd-smart-d300032-" IN URL
    // "http://www.walmart.com.ar/led-32-ilo-hd-smart-d300032-/p"
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

  /**
   * Average is calculated
   * 
   * @param document
   * @return
   */
  private Double getTotalAvgRating(Document docRating, Integer totalRating) {
    Double avgRating = 0.0;
    Elements rating = docRating.select("ul.rating li");

    if (totalRating != null) {
      Double total = 0.0;

      for (Element e : rating) {
        Element star = e.select("strong.rating-demonstrativo").first();
        Element totalStar = e.select("> span:not([class])").first();

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

  /**
   * Number of ratings appear in api
   * 
   * @param docRating
   * @return
   */
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
