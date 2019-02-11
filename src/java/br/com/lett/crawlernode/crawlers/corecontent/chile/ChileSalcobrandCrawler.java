package br.com.lett.crawlernode.crawlers.corecontent.chile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

public class ChileSalcobrandCrawler extends Crawler {

  public ChileSalcobrandCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"text/javascript\"]", "window.chaordic_meta = ", ";", false, false);
    JSONObject productJson = new JSONObject();
    JSONObject jsonPrices = CrawlerUtils.selectJsonFromHtml(doc, "script", "var prices = ", ";", false, true);
    JSONArray categoriesJson = new JSONArray();

    String description = crawlDesciption(doc);

    if (json.has("product")) {
      productJson = json.getJSONObject("product");
    }

    if (json.has("page")) {
      JSONObject pageJson = json.getJSONObject("page");

      if (pageJson.has("categories")) {
        categoriesJson = pageJson.getJSONArray("categories");
      }
    }

    CategoryCollection categories = crawlCategories(categoriesJson);

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      if (productJson.has("skus")) {

        for (Object obj : productJson.getJSONArray("skus")) {

          JSONObject sku = (JSONObject) obj;

          String internalId = crawlInternalId(sku);
          String internalPid = crawlInternalPid(sku);
          String name = crawlName(doc, internalId);
          boolean available = crawlAvailability(sku);
          Integer stock = scrapStock(sku);
          Float price = crawlPrice(jsonPrices, internalId);
          Prices prices = crawlPrices(jsonPrices, internalId);
          String primaryImage =
              CrawlerUtils.scrapSimplePrimaryImage(doc, "img[alt^=" + internalId + "]", Arrays.asList("data-src", "src"), "https:", "salcobrand.cl");
          String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "img[alt^=" + internalId + "]", Arrays.asList("data-src", "src"),
              "https:", "salcobrand.cl", primaryImage);
          String url = buildUrl(session.getOriginalURL(), internalId);

          Product product = ProductBuilder.create().setUrl(url).setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
              .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
              .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
              .setStock(stock).setMarketplace(new Marketplace()).build();

          products.add(product);
        }
      }


    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;

  }

  private String crawlName(Document doc, String internalId) {
    StringBuilder name = new StringBuilder();
    name.append(CrawlerUtils.scrapStringSimpleInfo(doc, ".product-content .info", false));

    Element selectElement = doc.selectFirst("#variant_id option[sku=" + internalId + "]");
    if (selectElement != null) {
      name.append(" ").append(selectElement.text().trim());
      Element quantityNameElement = doc.selectFirst(".input-group .first option[data-values-ids~=" + selectElement.val() + "]");

      if (quantityNameElement != null) {
        name.append(" ").append(quantityNameElement.text());
      }
    }

    return name.toString();
  }


  private String crawlDesciption(Document doc) {
    String description = null;
    Element descriptionElement = doc.selectFirst(".description");
    Element descriptionAreaElement = doc.selectFirst(".description-area");
    List<String> selectors = new ArrayList<>();

    selectors.add(".description");
    selectors.add(".description-area");

    if (descriptionElement != null && descriptionAreaElement != null) {
      description = CrawlerUtils.scrapSimpleDescription(doc, selectors);
    }

    return description;
  }

  private Float crawlPrice(JSONObject jsonPrices, String internalId) {
    Float price = null;

    if (jsonPrices.has(internalId)) {
      JSONObject priceObj = jsonPrices.getJSONObject(internalId);

      if (priceObj.has("normal") && priceObj.has("oferta")) {

        if (!priceObj.isNull("oferta")) {
          price = MathUtils.parseFloatWithComma(priceObj.get("oferta").toString().trim());

        } else {
          price = MathUtils.parseFloatWithComma(priceObj.get("normal").toString().trim());
        }
      }
    }

    return price;
  }

  private Prices crawlPrices(JSONObject jsonPrices, String internalId) {
    Prices prices = new Prices();
    Map<Integer, Float> installments = new HashMap<>();

    if (jsonPrices.has(internalId)) {
      JSONObject priceObj = jsonPrices.getJSONObject(internalId);

      if (priceObj.has("normal")) {
        prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceObj.get("normal").toString().trim()));

      }

      if (priceObj.has("tarjeta") && !priceObj.isNull("tarjeta")) {
        installments.put(1, MathUtils.parseFloatWithComma(priceObj.get("tarjeta").toString().trim()));

      } else if (priceObj.has("oferta") && !priceObj.isNull("oferta")) {
        installments.put(1, MathUtils.parseFloatWithComma(priceObj.get("oferta").toString().trim()));

      } else if (priceObj.has("normal") && !priceObj.isNull("normal")) {
        installments.put(1, MathUtils.parseFloatWithComma(priceObj.get("normal").toString().trim()));

      }
    }

    if (!installments.isEmpty()) {
      prices.insertCardInstallment(Card.VISA.toString(), installments);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installments);
    }

    return prices;
  }



  private CategoryCollection crawlCategories(JSONArray categoriesJson) {
    CategoryCollection categories = new CategoryCollection();

    for (Object object : categoriesJson) {

      JSONObject categorieJson = (JSONObject) object;

      if (categorieJson.has("name")) {
        categories.add(categorieJson.get("name").toString().trim());

      }

    }

    return categories;
  }

  private boolean crawlAvailability(JSONObject sku) {
    boolean availability = false;

    if (sku.has("status")) {
      availability = sku.get("status").toString().trim().equalsIgnoreCase("available");

    }

    return availability;
  }

  private Integer scrapStock(JSONObject sku) {
    Integer stock = 0;

    if (sku.has("stock")) {
      stock = sku.getInt("stock");
    }

    return stock;
  }

  private String crawlInternalPid(JSONObject sku) {
    String id = null;

    if (sku.has("id")) {
      id = sku.get("id").toString().trim();
    }

    return id;

  }

  private String crawlInternalId(JSONObject sku) {
    String id = null;

    if (sku.has("sku")) {
      id = sku.get("sku").toString().trim();
    }

    return id;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".big-product-container") != null;
  }

  private String buildUrl(String url, String productId) {
    String finalUrl = url;

    if (finalUrl != null) {
      finalUrl = finalUrl.substring(0, finalUrl.lastIndexOf('=') + 1) + productId;
    }

    return finalUrl;
  }
}
