package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXCrawlersUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

/**
 * date: 12/04/2018
 * 
 * @author gabriel
 *
 */
public class SaopauloDiaCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.dia.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "dia delivery";

  public SaopauloDiaCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }


  private static final String CEP = "04532040";

  @Override
  public void handleCookiesBeforeFetch() {
    Logging.printLogDebug(logger, session, "Adding cookie...");
    String url = "https://www.dia.com.br/api/checkout/pub/postal-code/BRA/" + CEP;
    this.cookies = CrawlerUtils.fetchCookiesFromAPage(url, null, "www.dia.com.br", "/", cookies, session, null, dataFetcher);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies, dataFetcher);
      vtexUtil.setHasBankTicket(false);

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

      String internalPid = vtexUtil.crawlInternalPid(skuJson);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb li:not(:first-child) a", true);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".productDescription"));

      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

      // ean data in json
      JSONArray arrayEan = CrawlerUtils.scrapEanFromVTEX(doc);

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = vtexUtil.crawlInternalId(jsonSku);
        JSONObject apiJSON = vtexUtil.crawlApi(internalId);
        String name = crawlName(jsonSku, skuJson, " - ");
        Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, false);
        Marketplace marketplace = vtexUtil.assembleMarketplaceFromMap(marketplaceMap);
        boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);
        String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
        String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
        Prices prices = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER) ? marketplaceMap.get(MAIN_SELLER_NAME_LOWER) : new Prices();
        Float price = vtexUtil.crawlMainPagePrice(prices);
        Integer stock = vtexUtil.crawlStock(apiJSON);
        String ean = i < arrayEan.length() ? arrayEan.getString(i) : null;

        List<String> eans = new ArrayList<>();
        eans.add(ean);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
            .setStock(stock).setMarketplace(marketplace).setEans(eans).build();

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

  public String crawlName(JSONObject jsonSku, JSONObject skuJson, String separator) {
    StringBuilder name = new StringBuilder();

    String nameVariation = jsonSku.has(VTEXCrawlersUtils.SKU_NAME) ? jsonSku.getString(VTEXCrawlersUtils.SKU_NAME) : null;

    if (skuJson.has(VTEXCrawlersUtils.PRODUCT_NAME)) {
      name.append(skuJson.getString(VTEXCrawlersUtils.PRODUCT_NAME));

      if (nameVariation != null) {
        if (name.length() > nameVariation.length()) {
          if (!name.toString().contains(nameVariation)) {
            name.append(separator).append(nameVariation);
          }
        } else {
          name = new StringBuilder().append(nameVariation);
        }
      }
    }

    return name.toString();
  }
}
