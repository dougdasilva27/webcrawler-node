package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public class BrasilEvinoCrawler extends Crawler {

  public BrasilEvinoCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      JSONObject skuJson = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"text/javascript\"]", "var TC = ", null, false, false);
      JSONObject productBiggyJson = skuJson.has("productBiggyJson") ? new JSONObject(skuJson.get("productBiggyJson").toString()) : new JSONObject();
      JSONObject productDetail = skuJson.has("productDetail") ? skuJson.getJSONObject("productDetail") : new JSONObject();
      System.err.println(productDetail);
      JSONObject images = productDetail.has("images") ? productDetail.getJSONObject("images") : new JSONObject();
      String internalPid = crawlInternalPid(skuJson);
      String name = crawlName(skuJson);

      String internalId = crawlInternalId(productDetail);
      Boolean available = crawlAvailability(productDetail);

      String primaryImage = crawlPrimaryImage(images);

      Float price = crawlPrice(productBiggyJson);

      CategoryCollection categories = null;// crawlCategories(doc);
      String description = null;// crawlDescription(doc);

      // Creating the product
      Product product =
          ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
              .setPrices(null).setAvailable(available).setCategory1(null).setCategory2(null).setCategory3(null).setPrimaryImage(primaryImage)
              .setSecondaryImages(null).setDescription(description).setStock(null).setMarketplace(null).setEans(null).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }

  private Float crawlPrice(JSONObject productBiggyJson) {
    return null;
  }

  private String crawlPrimaryImage(JSONObject images) {
    return images.has("large") ? images.getString("images") : null;
  }

  private Boolean crawlAvailability(JSONObject productDetail) {
    return productDetail.has("validForSale") ? productDetail.getBoolean("validForSale") : null;
  }

  private String crawlInternalId(JSONObject productDetail) {
    return productDetail.has("sku") ? productDetail.get("sku").toString() : null;
  }

  private String crawlName(JSONObject skuJson) {
    return skuJson.has("name") ? skuJson.get("name").toString() : null;
  }

  private String crawlInternalPid(JSONObject skuJson) {
    return skuJson.has("product") ? skuJson.get("product").toString() : null;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".Product") != null;
  }

}
