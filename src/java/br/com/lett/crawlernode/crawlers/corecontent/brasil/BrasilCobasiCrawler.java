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
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public class BrasilCobasiCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.cobasi.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "cobasi";

  public BrasilCobasiCrawler(Session session) {
    super(session);
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
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, logger, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies);
      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);
      String internalPid = vtexUtil.crawlInternalPid(skuJson);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb li > a");
      String description = crawlDescription(doc);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#image a", Arrays.asList("href"), "https:", "cobasi.vteximg.com.br");
      String secondaryImages = null;

      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = vtexUtil.crawlInternalId(jsonSku);
        JSONObject apiJSON = vtexUtil.crawlApi(internalId);
        String name = vtexUtil.crawlName(jsonSku, skuJson);
        Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId);
        Marketplace marketplace = vtexUtil.assembleMarketplaceFromMap(marketplaceMap);
        boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);
        primaryImage = (primaryImage == null) ? vtexUtil.crawlPrimaryImage(apiJSON) : primaryImage;
        secondaryImages = (secondaryImages == null) ? vtexUtil.crawlSecondaryImages(apiJSON) : secondaryImages;
        Prices prices = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER) ? marketplaceMap.get(MAIN_SELLER_NAME_LOWER) : new Prices();
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
    return document.select("#produto").first() != null;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Element shortDescription = doc.selectFirst(".productDescriptionShort");
    if (shortDescription != null) {
      description.append(shortDescription.html());
    }

    Element elementDescription = doc.select("#conteudo-produto").first();
    if (elementDescription != null) {
      description.append(elementDescription.html());
    }

    return description.toString();
  }
}
