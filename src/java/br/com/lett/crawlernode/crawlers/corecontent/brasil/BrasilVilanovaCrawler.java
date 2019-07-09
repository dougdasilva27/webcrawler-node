package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.prices.Prices;

/**
 * Date: 09/07/2019
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilVilanovaCrawler extends Crawler {

  public static final String HOME_PAGE = "https://www.vilanova.com.br/";
  private static final String IMAGES_HOST = "i2-vilanova.a8e.net.br";

  public BrasilVilanovaCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      JSONObject productJson = CrawlerUtils.selectJsonFromHtml(doc, "script", "skuJson = ", ";", false, true);

      String internalPid = crawlInternalPid(productJson);
      List<String> eans = Arrays.asList(CrawlerUtils.scrapStringSimpleInfo(doc, ".product-ean .value", true));
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".product-breadcrumb a:not(.first)");
      String description = CrawlerUtils.scrapElementsDescription(doc,
          Arrays.asList(".product-shape-and-volumn", ".specs-product dt .tab:not(.rating)", ".specs-product dl .content-tab:not(.ratings)"));
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "a.thumb-link", Arrays.asList("data-zoom-image", "href"), "https", IMAGES_HOST);
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "a.thumb-link", Arrays.asList("data-zoom-image", "href"),
          "https", IMAGES_HOST, primaryImage);

      JSONArray productsArray = productJson.has("skus") && !productJson.isNull("skus") ? productJson.getJSONArray("skus") : new JSONArray();

      for (Object obj : productsArray) {
        JSONObject skuJson = (JSONObject) obj;

        String internalId = crawlInternalId(skuJson);
        String name = crawlName(skuJson);

        Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrices(new Prices())
            .setAvailable(false)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
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
    return !doc.select(".infoProduct").isEmpty();
  }

  private String crawlInternalId(JSONObject skuJson) {
    String internalId = null;

    if (skuJson.has("sku") && !skuJson.isNull("sku")) {
      internalId = skuJson.get("sku").toString();
    }

    return internalId;
  }

  private String crawlInternalPid(JSONObject productJson) {
    String internalPid = null;

    if (productJson.has("productId") && !productJson.isNull("productId")) {
      internalPid = productJson.get("productId").toString();
    }

    return internalPid;
  }

  private String crawlName(JSONObject skuJson) {
    String name = null;

    if (skuJson.has("skuname") && skuJson.get("skuname") instanceof String) {
      name = skuJson.getString("skuname");
    }

    return name;
  }
}
