package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public class BrasilBenoitCrawler extends Crawler {

  public BrasilBenoitCrawler(Session session) {
    super(session);
    // TODO Auto-generated constructor stub
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    String url = session.getOriginalURL().concat(".json");

    JSONObject json = DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, url, null, cookies);

    String description = crawlDesciption(doc);

    Object categoriesJson = null;

    CategoryCollection categories = crawlCategories(categoriesJson);

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      if (json.has("Model")) {
        JSONObject model = json.getJSONObject("Model");
        String internalPid = model.has("ProductID") ? model.get("ProductID").toString() : null;
        Float price = crawlPrice(model);

        for (Object obj : model.getJSONArray("Items")) {

          JSONObject sku = (JSONObject) obj;

          String internalId = sku.has("SKU") ? sku.get("SKU").toString() : null;
          String name = crawlName(sku);
          boolean available = crawlAvailability(sku);
          Integer stock = scrapStock(sku);
          Prices prices = crawlPrices(json, internalId);
          String primaryImage =
              CrawlerUtils.scrapSimplePrimaryImage(doc, "img[alt^=" + internalId + "]", Arrays.asList("data-src", "src"), "https:", "salcobrand.cl");
          String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "img[alt^=" + internalId + "]", Arrays.asList("data-src", "src"),
              "https:", "salcobrand.cl", primaryImage);

          Product product = ProductBuilder.create().setUrl(url).setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
              .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
              .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
              .setStock(stock).setMarketplace(new Marketplace()).build();

          products.add(product);
        }
      }


    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private String crawlName(JSONObject sku) {
    String name = sku.has("Name") ? sku.get("Name").toString() : null;

    if (sku.has("Options") && name != null) {
      JSONArray options = sku.getJSONArray("Options");
      for (Object object : options) {
        JSONArray values = (JSONArray) object;
        for (Object object_ : values) {
          JSONObject value = (JSONObject) object_;
          if (value.has("Text")) {
            name.concat(" ").concat(value.getString("Text"));
          }
        }
      }
    }

    return name;
  }

  private Prices crawlPrices(JSONObject json, String internalId) {
    // TODO Auto-generated method stub
    return null;
  }

  private Float crawlPrice(JSONObject json) {
    JSONObject priceJson = json.getJSONObject("Price");
    JSONObject bestInstallment = priceJson.getJSONObject("BestInstallment");
    Float price = null;
    if (bestInstallment.has("InstallmentPrice")) {
      price = bestInstallment.getFloat("InstallmentPrice");
    }
    return price;
  }

  private Integer scrapStock(JSONObject sku) {
    // TODO Auto-generated method stub
    return null;
  }

  private boolean crawlAvailability(JSONObject sku) {
    boolean availability = false;

    if (sku.get("Availability").toString().equals("I")) {
      availability = true;
    }

    return availability;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".x-product-top-main") != null;
  }

  private CategoryCollection crawlCategories(Object categoriesJson) {
    // TODO Auto-generated method stub
    return null;
  }

  private String crawlDesciption(Document doc) {
    // TODO Auto-generated method stub
    return null;
  }
}
