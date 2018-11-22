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

public class ArgentinaJumboCrawler extends Crawler {

  public ArgentinaJumboCrawler(Session session) {
    super(session);
  }

  private static final String HOME_PAGE = "https://www.jumbo.com.ar/";
  private static final String MAIN_SELLER_NAME_LOWER = "jumbo argentina";
  private static final String MAIN_SELLER_NAME_LOWER_2 = "jumboargentina";

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
      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies);

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

      String internalPid = vtexUtil.crawlInternalPid(skuJson);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb li > a", true);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".productDescription", "#caracteristicas table"));

      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = vtexUtil.crawlInternalId(jsonSku);
        JSONObject apiJSON = vtexUtil.crawlApi(internalId);
        String name = vtexUtil.crawlName(jsonSku, skuJson);
        Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, true);
        Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap,
            Arrays.asList(MAIN_SELLER_NAME_LOWER, MAIN_SELLER_NAME_LOWER_2), Card.VISA, session);
        boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER) || marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER_2);
        String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
        String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
        Prices prices = CrawlerUtils.getPrices(marketplaceMap, Arrays.asList(MAIN_SELLER_NAME_LOWER, MAIN_SELLER_NAME_LOWER_2));
        Float price = vtexUtil.crawlMainPagePrice(prices);
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


  private boolean isProductPage(Document document) {
    return document.selectFirst(".productName") != null;
  }
}
