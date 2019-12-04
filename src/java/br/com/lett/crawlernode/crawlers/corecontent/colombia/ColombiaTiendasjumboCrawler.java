package br.com.lett.crawlernode.crawlers.corecontent.colombia;

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

public class ColombiaTiendasjumboCrawler extends Crawler {
  
  private static final String HOME_PAGE = "https://www.tiendasjumbo.co/";
  private static final String MAIN_SELLER_NAME_LOWER = "jumbo colombia";
  private static final String MAIN_SELLER_NAME_LOWER_2 = "jumbo colombia food";
  private static final List<String> SELLERS = Arrays.asList(MAIN_SELLER_NAME_LOWER, MAIN_SELLER_NAME_LOWER_2);

  public ColombiaTiendasjumboCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies, dataFetcher);

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);
      
      
      String internalPid = vtexUtil.crawlInternalPid(skuJson);
      //System.err.println(internalPid);

      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb li > a", true);
      String description = CrawlerUtils.scrapSimpleDescription(doc,
          Arrays.asList(".description", "#caracteristicas"));

      // sku data in json
      JSONArray arraySkus =
          skuJson != null &&
              skuJson.has("skus") &&
              !skuJson.isNull("skus")
                  ? skuJson.getJSONArray("skus")
                  : new JSONArray();

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);
        //System.err.println(jsonSku);
        
        String internalId = vtexUtil.crawlInternalId(jsonSku);
        JSONObject apiJSON = vtexUtil.crawlApi(internalId);
        String name = vtexUtil.crawlName(jsonSku, skuJson, " ");
        String primaryImage = jsonSku.getString("image");
        String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
        Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, false);
        Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, SELLERS, Card.VISA, session);
        boolean available = CrawlerUtils.getAvailabilityFromMarketplaceMap(marketplaceMap, SELLERS);
        Prices prices = CrawlerUtils.getPrices(marketplaceMap, SELLERS);
        Float price = vtexUtil.crawlMainPagePrice(prices);
        Integer stock = vtexUtil.crawlStock(apiJSON);
        JSONArray eanArray = CrawlerUtils.scrapEanFromVTEX(doc);

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
            .build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }
    return products;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".inner.product") != null;
  }
}
