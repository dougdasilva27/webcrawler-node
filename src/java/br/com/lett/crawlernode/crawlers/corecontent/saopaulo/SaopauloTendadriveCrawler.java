package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 04/09/17
 * 
 * @author gabriel
 *
 */

public class SaopauloTendadriveCrawler extends Crawler {

  private static final String HOME_PAGE = "http://www.tendadrive.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "tenda drive";

  public SaopauloTendadriveCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }


  @Override
  public void handleCookiesBeforeFetch() {
    Logging.printLogDebug(logger, session, "Adding cookie...");

    // shop id (AV guarapiranga)
    BasicClientCookie cookie = new BasicClientCookie("VTEXSC", "sc=10");
    cookie.setDomain(".www.tendadrive.com.br");
    cookie.setPath("/");
    this.cookies.add(cookie);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      VTEXCrawlersUtils vtexUtil =
          new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies);

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);
      String internalPid = vtexUtil.crawlInternalPid(skuJson);

      JSONObject skusInfo = crawlSKusInfo(internalPid);
      CategoryCollection categories = crawlCategories(skusInfo);
      String description = crawlDescription(skusInfo, doc);

      // sku data in json
      JSONArray arraySkus =
          skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

      // ean data in html
      JSONArray arrayEan = CrawlerUtils.scrapEanFromVTEX(doc);

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = vtexUtil.crawlInternalId(jsonSku);
        JSONObject apiJSON = vtexUtil.crawlApi(internalId);
        String name = vtexUtil.crawlName(jsonSku, skuJson);
        Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, true);
        Marketplace marketplace = vtexUtil.assembleMarketplaceFromMap(marketplaceMap);
        boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);
        String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
        String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
        Prices prices = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER)
            ? marketplaceMap.get(MAIN_SELLER_NAME_LOWER)
            : new Prices();
        Float price = vtexUtil.crawlMainPagePrice(prices);
        Integer stock = vtexUtil.crawlStock(apiJSON);
        String ean = i < arrayEan.length() ? arrayEan.getString(i) : null;

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL())
            .setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
            .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages)
            .setDescription(description).setStock(stock).setMarketplace(marketplace).build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }

  private boolean isProductPage(Document document) {
    return document.select(".produto").first() != null;
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

  private String crawlDescription(JSONObject skuInfo, Document doc) {
    StringBuilder description = new StringBuilder();

    if (skuInfo.has("description")) {
      description.append(skuInfo.getString("description") + "<br><br>");
    }

    Element info = doc.select("#caracteristicas").first();

    if (info != null) {
      info.select(".Avancado, .Caracteristicas").remove();
      description.append(info.html());
    }

    return description.toString();
  }

  private JSONObject crawlSKusInfo(String internalPid) {
    JSONObject info = new JSONObject();

    String url = "http://www.tendadrive.com.br/api/catalog_system/pub/products/search?fq=productId:"
        + internalPid + "&sc=14";
    JSONArray skus =
        DataFetcher.fetchJSONArray(DataFetcher.GET_REQUEST, session, url, null, cookies);

    if (skus.length() > 0) {
      info = skus.getJSONObject(0);
    }

    return info;
  }
}
