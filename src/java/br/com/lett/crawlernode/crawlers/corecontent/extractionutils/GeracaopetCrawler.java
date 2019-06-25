package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.ArrayList;
import java.util.List;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
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
      Product product = new Product();

      JSONObject skuJson = crawlSkuJson(doc);

      String internalPid = crawlInternalPid(doc);
      String description = crawlDescription(doc);

      JSONObject options = crawlOptions(skuJson);
      if (options.length() > 0) {

        for (String keyStr : options.keySet()) {
          JSONObject jsonSku = (JSONObject) options.get(keyStr);

          boolean available = crawlAvailability(skuJson);
          String internalId = crawlInternalId(jsonSku);
          String name = crawlName(doc, skuJson);

          String primaryImage = crawlPrimaryImage(skuJson);
          String secondaryImages = crawlSecondaryImages(skuJson);
          Float price = crawlPrice(skuJson);
          Prices prices = crawlPrices(skuJson);
          Integer stock = null;

          // Creating the product
          product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
              .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(null).setCategory2(null).setCategory3(null)
              .setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description).setStock(stock).setMarketplace(null)
              .setEans(null).build();

          products.add(product);

          // Map<String, List<String>> variationsMap = new HashMap<>();
          //
          // if (variationsMap.containsKey("internalId")) {
          // List<String> names = variationsMap.get("internalId");
          // names.add("label");
          //
          // variationsMap.put("internalId", names);
          // } else {
          // variationsMap.put("internalId", Arrays.asList("label"));
          // }

        }
      } else {
        // SEM VARIAÇÃO
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
    }

    return products;
  }

  private JSONObject crawlOptions(JSONObject skuJson) {
    JSONObject optionPrices = new JSONObject();

    if (skuJson.has("jsonConfig")) {
      JSONObject jsonConfig = skuJson.getJSONObject("jsonConfig");

      if (jsonConfig.has("optionPrices")) {
        optionPrices = jsonConfig.getJSONObject("optionPrices");
      }
    }

    return optionPrices;
  }

  private boolean crawlAvailability(JSONObject skuJson) {
    boolean availability = false;

    if (skuJson.has("jsonConfig")) {
      JSONObject jsonConfig = skuJson.getJSONObject("jsonConfig");

      if (jsonConfig.has("attributes")) {
        JSONObject attributes = jsonConfig.getJSONObject("attributes");

        for (String keyStr : attributes.keySet()) {
          JSONObject keyValue = (JSONObject) attributes.get(keyStr);

        }
      }
    }
    return availability;
  }

  private Float crawlPrice(JSONObject skuJson) {
    return null;
  }

  private Prices crawlPrices(JSONObject skuJson) {
    return null;
  }

  private String crawlSecondaryImages(JSONObject skuJson) {
    return null;
  }

  private String crawlPrimaryImage(JSONObject skuJson) {
    return null;
  }

  // COMPLETAR O NOME COM AS VARIAÇÕES

  private String crawlName(Document doc, JSONObject skuJson) {
    Element title = doc.selectFirst("h1 span[itemprop=\"name\"]");
    String name = null;

    if (title != null) {
      name = title.text();
    }

    return name;
  }

  private String crawlInternalId(JSONObject jsonSku) {
    return null;
  }

  private String crawlDescription(Document doc) {

    Element div = doc.selectFirst(".data.item.content");
    String description = null;

    if (div != null) {
      description = div.html();
    }

    return description;
  }

  private String crawlInternalPid(Document doc) {
    String internalPid = null;
    Element div = doc.selectFirst("div[data-product-id]");

    if (div != null) {
      internalPid = div.attr("data-product-id");
    }

    return internalPid;
  }

  private JSONObject crawlSkuJson(Document doc) {
    JSONObject skuJson = new JSONObject();
    Element script = doc.selectFirst(".fieldset script[type=\"text/x-magento-init\"]");

    if (script != null) {
      skuJson = new JSONObject(script.html());

    } else {
      script = doc.selectFirst(".media script[type=\"text/x-magento-init\"]");

      if (script != null) {
        skuJson = new JSONObject(script.html());
      }
    }

    return skuJson;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst("#maincontent") != null;
  }
}
