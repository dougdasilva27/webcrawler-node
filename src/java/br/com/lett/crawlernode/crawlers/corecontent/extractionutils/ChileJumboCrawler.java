package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public class ChileJumboCrawler {

  public static final String JUMBO_DEHESA_ID = "13";
  public static final String JUMBO_LAFLORIDA_ID = "18";
  public static final String JUMBO_LAREINA_ID = "11";
  public static final String JUMBO_LOSDOMINICOS_ID = "12";
  public static final String JUMBO_VINA_ID = "16";
  
  public static final String RANKING_SELECTOR = "li[layout] .product-item";
  public static final String RANKING_SELECTOR_URL = ".product-item__info a";
  public static final String RANKING_SELECTOR_TOTAL = ".resultado-busca-numero";
  public static final String RANKING_ATTRIBUTE_ID = "data-id";

  private static final String MAIN_SELLER_NAME_LOWER = "jumbo";
  public static final String HOME_PAGE = "https://nuevo.jumbo.cl/";
  public static final String HOST = "nuevo.jumbo.cl";
  private Session session;
  private List<Cookie> cookies = new ArrayList<>();
  private Logger logger;

  public ChileJumboCrawler(Session session, List<Cookie> cookies, Logger logger) {
    this.session = session;
    this.cookies = cookies;
    this.logger = logger;
  }

  public List<Product> extractProducts(Document doc) {
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, this.cookies);
      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

      String internalPid = vtexUtil.crawlInternalPid(skuJson);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb li:not(:first-child) > a", false);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".productDescription", "#caracteristicas table.Ficha-Tecnica"));

      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = vtexUtil.crawlInternalId(jsonSku);
        JSONObject apiJSON = vtexUtil.crawlApi(internalId);
        String name = vtexUtil.crawlName(jsonSku, skuJson, apiJSON);
        Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, false);
        List<String> mainSellers = CrawlerUtils.getMainSellers(marketplaceMap, Arrays.asList(MAIN_SELLER_NAME_LOWER));
        Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, mainSellers, Card.AMEX, session);
        boolean available = CrawlerUtils.getAvailabilityFromMarketplaceMap(marketplaceMap, mainSellers);
        String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
        String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
        Prices prices = CrawlerUtils.getPrices(marketplaceMap, mainSellers);
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
      Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
    }

    return products;
  }

  private static boolean isProductPage(Document document) {
    return document.select(".product-content").first() != null;
  }
}
