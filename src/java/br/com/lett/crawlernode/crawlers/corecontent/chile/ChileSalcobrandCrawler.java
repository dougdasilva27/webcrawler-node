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
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
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

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalPid = scrapInternalPid(doc);
      JSONArray skusStock = fetchStockAPI(internalPid);
      JSONObject skusPrices = CrawlerUtils.selectJsonFromHtml(doc, "script", "var prices = ", ";", false, true);
      String description = crawlDesciption(doc);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li  a:not([href=\"/\"])");

      // Json skusPrices Ex:
      // {"4750152":{"normal":"$3.299","oferta":null,"internet":null,"tarjeta":null},"4750155":{"normal":"$5.599","oferta":null,"internet":null,"tarjeta":null}}
      // When 4750152 and 4750155 are internalId's
      for (String internalId : skusPrices.keySet()) {

        String name = crawlName(doc, internalId);
        Integer stock = scrapStock(internalId, skusStock);
        boolean available = stock > 0;
        Float price = crawlPrice(skusPrices, internalId);
        Prices prices = crawlPrices(skusPrices, internalId);
        String primaryImage =
            CrawlerUtils.scrapSimplePrimaryImage(doc, "img[alt^=" + internalId + "]", Arrays.asList("data-src", "src"), "https:", "salcobrand.cl");
        String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "img[alt^=" + internalId + "]", Arrays.asList("data-src", "src"),
            "https:", "salcobrand.cl", primaryImage);
        String url = buildUrl(session.getOriginalURL(), internalId);

        Product product = ProductBuilder.create()
            .setUrl(url)
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
            .setMarketplace(new Marketplace())
            .setStock(stock)
            .build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private Integer scrapStock(String internalId, JSONArray skusStock) {
    Integer stock = 0;

    for (Object obj : skusStock) {
      JSONObject skuStock = (JSONObject) obj;

      if (skuStock.has(internalId)) {
        stock = CrawlerUtils.getIntegerValueFromJSON(skuStock, internalId, 0);
        break;
      }
    }

    return stock;
  }

  /**
   * Ex:
   * 
   * [{"4751790":0},{"4751792":1548}]
   * 
   * @param internalPid
   * @return
   */
  private JSONArray fetchStockAPI(String internalPid) {
    String url = "https://salcobrand.cl/api/v1/stock?sku=" + internalPid;
    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();

    return CrawlerUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());
  }

  private String scrapInternalPid(Document doc) {
    String internalPid = null;

    String token = "\"sku=";

    Elements scripts = doc.select("script");
    for (Element e : scripts) {
      String text = e.html().toLowerCase().replace(" ", "");

      if (text.contains(token)) {
        int x = text.indexOf(token) + token.length();

        String id = text.substring(x).trim();
        if (id.contains("\"")) {
          int y = id.indexOf('"');

          internalPid = id.substring(0, y);
        } else {
          internalPid = id;
        }

        break;
      }
    }

    return internalPid;
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
    List<String> selectors = new ArrayList<>();

    selectors.add(".description");
    selectors.add("#description .description-area");
    description = CrawlerUtils.scrapSimpleDescription(doc, selectors);

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
