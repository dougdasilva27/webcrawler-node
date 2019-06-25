package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.prices.Prices;

public class GeracaopetCrawler extends Crawler {

  protected String cep;

  public GeracaopetCrawler(Session session, String cep) {
    super(session);
    this.cep = cep;
  }

  @Override
  public void handleCookiesBeforeFetch() {
    BasicClientCookie cookie = new BasicClientCookie("zipcode", cep);
    cookie.setDomain(".www.geracaopet.com.br");
    cookie.setPath("/");
    this.cookies.add(cookie);
  }

  @Override
  public List<Product> extractInformation(Document doc) {
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + session.getOriginalURL());

      JSONObject skuJson = crawlSkuJson(doc);

      String internalPid = crawlInternalPid(skuJson);
      CategoryCollection categories = crawlCategories(doc);
      String description = crawlDescription(doc, internalPid);

      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

      // ean data in json
      JSONArray arrayEan = CrawlerUtils.scrapEanFromVTEX(doc);

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);
        boolean available = crawlAvailability(skuJson);
        String internalId = crawlInternalId(jsonSku);
        String name = crawlName(jsonSku, skuJson);
        Map<String, Float> marketplaceMap = crawlMarketplace(jsonSku);

        String primaryImage = crawlPrimaryImage(skuJson);
        String secondaryImages = crawlSecondaryImages(skuJson);
        Float price = crawlPrice(skuJson);
        Prices prices = crawlPrices(skuJson);
        Integer stock = null;
        String ean = i < arrayEan.length() ? arrayEan.getString(i) : null;
        List<String> eans = new ArrayList<>();
        eans.add(ean);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
            .setStock(stock).setMarketplace(null).setEans(eans).build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
    }

    return products;
  }

  private boolean crawlAvailability(JSONObject skuJson) {
    // TODO Auto-generated method stub
    return false;
  }

  private Float crawlPrice(JSONObject skuJson) {
    // TODO Auto-generated method stub
    return null;
  }

  private Prices crawlPrices(JSONObject skuJson) {
    // TODO Auto-generated method stub
    return null;
  }

  private String crawlSecondaryImages(JSONObject skuJson) {
    // TODO Auto-generated method stub
    return null;
  }

  private String crawlPrimaryImage(JSONObject skuJson) {
    // TODO Auto-generated method stub
    return null;
  }

  private Map<String, Float> crawlMarketplace(JSONObject jsonSku) {
    // TODO Auto-generated method stub
    return null;
  }

  private String crawlName(JSONObject jsonSku, JSONObject skuJson) {
    // TODO Auto-generated method stub
    return null;
  }

  private String crawlInternalId(JSONObject jsonSku) {
    // TODO Auto-generated method stub
    return null;
  }

  private String crawlDescription(Document doc, String internalPid) {
    // TODO Auto-generated method stub
    return null;
  }

  private CategoryCollection crawlCategories(Document doc) {
    // TODO Auto-generated method stub
    return null;
  }

  private String crawlInternalPid(JSONObject skuJson) {
    // TODO Auto-generated method stub
    return null;
  }

  private JSONObject crawlSkuJson(Document doc) {
    JSONObject skuJson = new JSONObject();
    Element script = doc.selectFirst(".fieldset script[type=\"text/x-magento-init\"]");

    if (script != null) {
      skuJson = new JSONObject(script.html());
    }

    System.err.println(skuJson);
    return skuJson;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst("#maincontent") != null;
  }
}
