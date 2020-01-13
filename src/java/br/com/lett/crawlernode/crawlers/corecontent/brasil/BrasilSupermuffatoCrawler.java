package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilSupermuffatoCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.supermuffato.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "super muffato eletro";

  public BrasilSupermuffatoCrawler(Session session) {
    super(session);
    super.config.setMustSendRatingToKinesis(true);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }


  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies, dataFetcher);

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

      String internalPid = vtexUtil.crawlInternalPid(skuJson);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb > ul li a");
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#prd-description", "#prd-specifications"));

      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

      // ean data in json
      JSONArray arrayEans = CrawlerUtils.scrapEanFromVTEX(doc);

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = vtexUtil.crawlInternalId(jsonSku);
        JSONObject apiJSON = vtexUtil.crawlApi(internalId);
        String name = vtexUtil.crawlName(jsonSku, skuJson, " ");
        Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, true);
        Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, Arrays.asList(MAIN_SELLER_NAME_LOWER), Card.VISA, session);
        boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);
        String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
        String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
        Prices prices = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER) ? marketplaceMap.get(MAIN_SELLER_NAME_LOWER) : new Prices();
        Float price = vtexUtil.crawlMainPagePrice(prices);
        Integer stock = vtexUtil.crawlStock(apiJSON);
        String ean = i < arrayEans.length() ? arrayEans.getString(i) : null;
        RatingsReviews ratingReviews = extractRatingAndReviews(doc);

        List<String> eans = new ArrayList<>();
        eans.add(ean);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
            .setStock(stock).setMarketplace(marketplace).setEans(eans).setRatingReviews(ratingReviews).build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private boolean isProductPage(Document document) {
    return document.selectFirst(".productName") != null;
  }



  protected RatingsReviews extractRatingAndReviews(Document doc) {
    RatingsReviews ratingReviews = new RatingsReviews();
    ratingReviews.setDate(session.getDate());

    JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

    if (skuJson.has("productId")) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      List<String> idList = VTEXCrawlersUtils.crawlIdList(skuJson);
      for (String internalId : idList) {

        JSONObject rating = crawlProductInformatioApi(internalId);

        Integer totalReviews = CrawlerUtils.getIntegerValueFromJSON(rating, "count", 0);

        Double avgRating = CrawlerUtils.getDoubleValueFromJSON(rating, "rate", true, false);

        ratingReviews.setTotalRating(totalReviews);
        ratingReviews.setTotalWrittenReviews(totalReviews);
        ratingReviews.setAverageOverallRating(avgRating == null ? 0d : avgRating);
        ratingReviews.setInternalId(internalId);
      }

    }

    return ratingReviews;

  }

  private JSONObject crawlProductInformatioApi(String internalId) {
    JSONObject ratingJson = new JSONObject();

    String apiUrl = "https://awsapis3.netreviews.eu/product";
    String payload =
        "{\"query\":\"average\",\"products\":[\"" + internalId + "\"],\"idWebsite\":\"4f870cb3-d6ef-5664-2950-de136d5b471e\",\"plateforme\":\"br\"}";
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json; charset=UTF-8");

    Request request =
        RequestBuilder.create().setUrl(apiUrl).setCookies(cookies).setHeaders(headers).setPayload(payload).mustSendContentEncoding(false).build();
    JSONObject response = CrawlerUtils.stringToJson(new FetcherDataFetcher().post(session, request).getBody());

    if (response.has(internalId)) {
      JSONArray rate = response.getJSONArray(internalId);

      if (rate.length() > 0) {
        ratingJson = rate.getJSONObject(0);
      }
    }

    return ratingJson;
  }
}
