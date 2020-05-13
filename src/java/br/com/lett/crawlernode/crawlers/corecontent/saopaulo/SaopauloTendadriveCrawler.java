package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import models.RatingsReviews;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

/**
 * Date: 04/09/17
 *
 * @author gabriel
 */
public class SaopauloTendadriveCrawler extends Crawler {

  private static final String HOME_PAGE = "http://www.tendaatacado.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "tenda drive";

  public SaopauloTendadriveCrawler(Session session) {
    super(session);
    super.config.setMustSendRatingToKinesis(true);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public void handleCookiesBeforeFetch() {
    Logging.printLogDebug(logger, session, "Adding cookie...");

    this.cookies.addAll(
        CrawlerUtils.fetchCookiesFromAPage(
            HOME_PAGE, null, ".www.tendaatacado.com.br", "/", cookies, session, dataFetcher));

    // shop id (AV guarapiranga)
    BasicClientCookie cookie2 = new BasicClientCookie("VTEXSC", "sc=1");
    cookie2.setDomain(".www.tendaatacado.com.br");
    cookie2.setPath("/");
    this.cookies.add(cookie2);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {

      JSONObject jsonObject = JSONUtils.stringToJson(doc.selectFirst("#__NEXT_DATA__").data());

      JSONObject skuJson = (JSONObject) jsonObject.optQuery("/props/pageProps/product");

      String internalPid = skuJson.optString("name");

      JSONObject skusInfo = crawlSKusInfo(internalPid);

      List<String> categories = doc.select(".breadcrumbs a").eachText();
      categories.remove(0);
      String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(""));

      String internalId = scrapInternal(skuJson);
      String name = skuJson.optString("name");
      List<String> images = scrapImages(skuJson);
      String primaryImage = images.remove(0);
      String secondaryImages = new JSONArray(images).toString();
      Integer stock = skuJson.optInt("totalStock");
      RatingsReviews ratingsReviews = scrapRating(internalId, doc);

      Product product =
          ProductBuilder.create()
              .setUrl(session.getOriginalURL())
              .setInternalId(internalId)
              .setInternalPid(internalPid)
              .setName(name)
              .setCategories(categories)
              .setPrimaryImage(primaryImage)
              .setSecondaryImages(secondaryImages)
              .setDescription(description)
              .setStock(stock)
              .setRatingReviews(ratingsReviews)
              .build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private List<String> scrapImages(JSONObject skuJson) {
    JSONArray photos = skuJson.optJSONArray("photos");
    List<String> images = new ArrayList<>();
    for (Object obj : photos) {
      if (obj instanceof JSONObject) {
        JSONObject json = (JSONObject) obj;
        images.add(json.optString("url", null));
      }
    }
    return images;
  }

  private String scrapInternal(JSONObject skuJson) {
    String[] tokens = skuJson.optString("token").split("-");
    return tokens[tokens.length - 1];
  }

  private boolean isProductPage(Document document) {
    return document.selectFirst(".box-product") != null;
  }

  private CategoryCollection crawlCategories(JSONObject skuinfo) {
    CategoryCollection categories = new CategoryCollection();

    if (skuinfo.has("categories")) {
      JSONArray cats = skuinfo.getJSONArray("categories");

      for (int i = cats.length() - 1; i >= 0; i--) {
        String cat = cats.getString(i) + " ";
        String[] tokens = cat.split("/");

        categories.add(tokens[tokens.length - 2]);
      }
    }

    return categories;
  }

  private JSONObject crawlSKusInfo(String internalPid) {
    JSONObject info = new JSONObject();

    String url =
        "https://www.tendaatacado.com.br/api/catalog_system/pub/products/search?fq=productId:"
            + internalPid
            + "&sc=";
    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
    JSONArray skus =
        CrawlerUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());

    if (skus.length() > 0) {
      info = skus.getJSONObject(0);
    }

    return info;
  }

  private RatingsReviews scrapRating(String internalId, Document doc) {
    TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, "80984", logger);
    return trustVox.extractRatingAndReviews(internalId, doc, dataFetcher);
  }
}
