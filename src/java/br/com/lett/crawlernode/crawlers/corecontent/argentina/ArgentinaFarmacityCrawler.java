package br.com.lett.crawlernode.crawlers.corecontent.argentina;

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
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 22/09/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class ArgentinaFarmacityCrawler extends Crawler {

  private static final String HOME_PAGE = "http://www.farmacity.com/";
  private static final String MAIN_SELLER_NAME_LOWER = "farmacity sa";

  public ArgentinaFarmacityCrawler(Session session) {
    super(session);
  }

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
      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies, dataFetcher);
      vtexUtil.setHasBankTicket(false);

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

      String internalPid = vtexUtil.crawlInternalPid(skuJson);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb li" + CrawlerUtils.CSS_SELECTOR_IGNORE_FIRST_CHILD);
      String description = crawlDescription(doc);

      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

      // ean data in html
      JSONArray arrayEan = CrawlerUtils.scrapEanFromVTEX(doc);

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = vtexUtil.crawlInternalId(jsonSku);
        String newUrl = internalId != null ? CrawlerUtils.getRedirectedUrl(session.getOriginalURL(), session) : session.getOriginalURL();
        JSONObject apiJSON = vtexUtil.crawlApi(internalId);
        String brand = CrawlerUtils.scrapStringSimpleInfo(doc, ".brand", false);

        // This crawler has name > variationName. We need to make a new function to scrap the name.
        String name = crawlName(jsonSku, skuJson, brand);

        Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId);
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
        Product product = ProductBuilder.create().setUrl(newUrl).setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
            .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
            .setStock(stock).setMarketplace(marketplace).setEans(eans).build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private String crawlName(JSONObject jsonSku, JSONObject skuJson, String brand) {
    StringBuilder name = new StringBuilder();

    if (skuJson.has("name")) {
      name.append(skuJson.getString("name"));
      // The lines below are commented because there are not name with variation in this site

      // String nameVariation = jsonSku.has("skuname") ? jsonSku.getString("skuname") : null;
      //
      // if (nameVariation != null) {
      // name.append(" ").append(nameVariation);
      // }
    }

    if (brand != null) {
      brand = brand + " ";
      name.insert(0, brand);
    }
    return name.toString();
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(Document doc) {
    return doc.select(".product-info-container").first() != null;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Element elementDescription = doc.select("div.product-description-box").first();

    if (elementDescription != null) {
      description.append(elementDescription.html());
    }

    Element elementSpecs = doc.select("div.product-specification-box").first();

    if (elementSpecs != null) {
      description.append(elementSpecs.html());
    }

    Element productDescription = doc.selectFirst(".product-description-container .productDescription");

    if (productDescription != null) {
      description.append(productDescription.text());
    }

    return description.toString();
  }

}
