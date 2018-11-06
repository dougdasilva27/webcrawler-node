package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
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
import models.prices.Prices;

public class ArgentinaPardoCrawler extends Crawler {

  public ArgentinaPardoCrawler(Session session) {
    super(session);
  }

  private static final String HOME_PAGE = "https://www.pardo.com.ar/";
  private static final String MAIN_SELLER_NAME_LOWER = "pardo | acá podes";
  private static final String MAIN_SELLER_NAME_LOWER_2 = "pardo hogar";

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
      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, logger, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies);

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

      String internalPid = vtexUtil.crawlInternalPid(skuJson);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs li > a", false);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".productDescription"));

      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = vtexUtil.crawlInternalId(jsonSku);
        JSONObject apiJSON = vtexUtil.crawlApi(internalId);
        String name = vtexUtil.crawlName(jsonSku, skuJson);
        Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, true);
        Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap,
            Arrays.asList(MAIN_SELLER_NAME_LOWER, MAIN_SELLER_NAME_LOWER_2), Card.AMEX, session);
        boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER) || marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER_2);
        String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
        String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
        Prices prices = getPrices(marketplaceMap);
        Float price = vtexUtil.crawlMainPagePrice(prices, Card.AMEX);
        Integer stock = vtexUtil.crawlStock(apiJSON);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
            .setStock(stock).setMarketplace(marketplace).build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }


  private Prices getPrices(Map<String, Prices> marketplaceMap) {
    Prices prices = new Prices();

    if (marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER)) {
      prices = marketplaceMap.get(MAIN_SELLER_NAME_LOWER);
    } else if (marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER_2)) {
      prices = marketplaceMap.get(MAIN_SELLER_NAME_LOWER_2);
    }

    return prices;
  }

  private boolean isProductPage(Document document) {
    return document.selectFirst(".productName") != null;
  }
}
