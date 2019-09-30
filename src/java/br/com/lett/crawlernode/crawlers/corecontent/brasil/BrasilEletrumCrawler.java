package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
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
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.YourreviewsRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.Offers;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilEletrumCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.eletrum.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "eletrum";
  private static final String STORE_KEY = "8ea7baa3-231d-4049-873e-ad5afd085ca4";

  public BrasilEletrumCrawler(Session session) {
    super(session);
    super.config.setMustSendRatingToKinesis(true);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies, 10, null, dataFetcher);

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

      String internalPid = vtexUtil.crawlInternalPid(skuJson);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb li" + CrawlerUtils.CSS_SELECTOR_IGNORE_FIRST_CHILD + " > a");
      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

      // ean data in json
      JSONArray arrayEans = CrawlerUtils.scrapEanFromVTEX(doc);

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = vtexUtil.crawlInternalId(jsonSku);
        JSONObject apiJSON = vtexUtil.crawlApi(internalId);
        String description = crawlDescription(internalPid, apiJSON, vtexUtil, doc);
        String name = vtexUtil.crawlName(jsonSku, skuJson, apiJSON);
        Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, false);
        Marketplace marketplace = vtexUtil.assembleMarketplaceFromMap(marketplaceMap);
        boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);
        String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
        String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
        Prices prices = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER) ? marketplaceMap.get(MAIN_SELLER_NAME_LOWER) : new Prices();
        Float price = vtexUtil.crawlMainPagePrice(prices);
        Integer stock = vtexUtil.crawlStock(apiJSON);
        String ean = i < arrayEans.length() ? arrayEans.getString(i) : null;
        Offers offers = vtexUtil.scrapBuyBox(apiJSON);
        List<String> eans = new ArrayList<>();
        eans.add(ean);
        RatingsReviews ratingReviews = crawlRatingReviews(internalPid);

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
            .setOffers(offers)
            .setRatingReviews(ratingReviews)
            .build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private RatingsReviews crawlRatingReviews(String internalPid) {
    RatingsReviews ratingReviews = new RatingsReviews();
    ratingReviews.setDate(session.getDate());

    YourreviewsRatingCrawler yourReviews = new YourreviewsRatingCrawler(session, cookies, logger, STORE_KEY, this.dataFetcher);

    Document docRating = yourReviews.crawlPageRatingsFromYourViews(internalPid, STORE_KEY, this.dataFetcher);
    Integer totalNumOfEvaluations = getTotalNumOfRatingsFromYourViews(docRating);
    Double avgRating = getTotalAvgRatingFromYourViews(docRating);
    AdvancedRatingReview advancedRatingReview = yourReviews.getTotalStarsFromEachValue(internalPid);
    ratingReviews.setAdvancedRatingReview(advancedRatingReview);
    ratingReviews.setTotalRating(totalNumOfEvaluations);
    ratingReviews.setAverageOverallRating(avgRating);
    ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);

    return ratingReviews;
  }

  private Double getTotalAvgRatingFromYourViews(Document docRating) {
    Double avgRating = 0d;
    Element rating = docRating.selectFirst("meta[itemprop=ratingValue]");

    if (rating != null) {
      Double avg = MathUtils.parseDoubleWithDot((rating.attr("content")));
      avgRating = avg != null ? avg : 0d;
    }

    return avgRating;
  }

  private Integer getTotalNumOfRatingsFromYourViews(Document docRating) {
    Integer totalRating = 0;
    Element totalRatingElement = docRating.selectFirst("meta[itemprop=ratingCount]");

    if (totalRatingElement != null) {
      Integer total = MathUtils.parseInt(totalRatingElement.attr("content"));
      totalRating = total != null ? total : 0;
    }

    return totalRating;
  }


  private String crawlDescription(String internalPid, JSONObject apiJSON, VTEXCrawlersUtils vtexUtil, Document doc) {
    StringBuilder description = new StringBuilder();
    JSONObject json = vtexUtil.crawlDescriptionAPI(internalPid, "productId");

    if (json.has("description")) {
      description.append("<div><h3>Descrição</h3></div>");
      description.append(json.get("description").toString());
    }

    if (json.has("Descrição Do Produto")) {
      JSONArray jsonArray = json.getJSONArray("Descrição Do Produto");
      List<String> listKeys = new ArrayList<>();

      for (Object object : jsonArray) {
        String keys = (String) object;
        listKeys.add(keys);
      }

      for (String string : listKeys) {
        if (json.has(string)) {
          description.append(string).append(": ").append(json.get(string).toString().replace("[", "").replace("]", "")).append("<br>");
        }
      }

    }

    return description.toString();
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".bf-product__spot") != null;
  }



}
