package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
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
      JSONObject jsonHtml = crawlJsonHtml(doc);

      JSONObject skuJson = crawlSkuJson(jsonHtml);

      String internalPid = crawlInternalPid(doc);
      String description = crawlDescription(doc);

      JSONObject options = crawlOptions(skuJson);

      if (options.length() > 0) {
        Map<String, Set<String>> variationsMap = crawlVariationsMap(skuJson);

        for (String keyStr : options.keySet()) {

          String internalId = keyStr;
          boolean available = crawlAvailability(variationsMap, internalId);
          String name = crawlName(doc, variationsMap, internalId);
          String primaryImage = crawlPrimaryImage(skuJson, internalId, available);

          String secondaryImages = crawlSecondaryImages(skuJson, internalId, available);
          Float price = crawlPrice(options, internalId, available);
          Prices prices = crawlPrices(options, internalId, available);
          Integer stock = null;

          // Creating the product
          Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid)
              .setName(name).setPrice(price).setPrices(prices).setAvailable(available).setCategory1(null).setCategory2(null).setCategory3(null)
              .setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description).setStock(stock)
              .setMarketplace(new Marketplace()).setEans(null).build();

          products.add(product);

        }
      } else {
        // SEM VARIAÇÃO
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
    }

    return products;
  }

  private boolean crawlAvailability(Map<String, Set<String>> variationsMap, String internalId) {
    boolean availability = false;

    if (variationsMap.containsKey(internalId)) {
      String name = variationsMap.get(internalId).toString();
      if (!name.contains("disabled")) {
        availability = true;
      }
    }
    return availability;
  }

  private JSONObject crawlSkuJson(JSONObject jsonHtml) {
    JSONObject skuJson = new JSONObject();

    if (jsonHtml.has("[data-role=swatch-options]")) {
      JSONObject dataSwatch = jsonHtml.getJSONObject("[data-role=swatch-options]");

      if (dataSwatch.has("Magento_Swatches/js/swatch-renderer")) {
        skuJson = dataSwatch.getJSONObject("Magento_Swatches/js/swatch-renderer");
      }
    }


    return skuJson;
  }

  private Map<String, Set<String>> crawlVariationsMap(JSONObject skuJson) {
    Map<String, Set<String>> variationsMap = new HashMap<>();
    JSONArray options = new JSONArray();

    if (skuJson.has("jsonConfig")) {
      JSONObject jsonConfig = skuJson.getJSONObject("jsonConfig");

      if (jsonConfig.has("attributes")) {
        JSONObject attributes = jsonConfig.getJSONObject("attributes");

        for (String keyStr : attributes.keySet()) {
          JSONObject attribute = (JSONObject) attributes.get(keyStr);

          if (attribute.has("options")) {
            options = attribute.getJSONArray("options");
          }
        }
      }
    }

    for (Object object : options) {
      JSONObject option = (JSONObject) object;
      String label = null;
      if (option.has("label")) {
        label = option.getString("label");
      }

      if (option.has("products")) {
        JSONArray products = option.getJSONArray("products");

        for (Object object2 : products) {
          String id = (String) object2;

          if (variationsMap.containsKey(id)) {
            Set<String> names = variationsMap.get(id);
            Set<String> newList = new HashSet<>(names);
            newList.add(label);
            variationsMap.put(id, newList);
          } else {
            Set<String> newSet = new HashSet<>();
            newSet.add(label);
            variationsMap.put(id, newSet);
          }
        }
      }
    }
    return variationsMap;

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

  private Float crawlPrice(JSONObject jsonSku, String internalId, boolean available) {
    Float price = null;

    if (available && jsonSku.has(internalId)) {
      JSONObject eachPrice = jsonSku.getJSONObject(internalId);

      if (eachPrice.has("finalPrice")) {
        JSONObject finalPrice = eachPrice.getJSONObject("finalPrice");

        if (finalPrice.has("amount")) {
          price = CrawlerUtils.getFloatValueFromJSON(finalPrice, "amount");
        }
      }
    }
    return price;
  }

  private Prices crawlPrices(JSONObject jsonSku, String internalId, boolean available) {
    Prices prices = new Prices();
    Map<Integer, Float> installmentPrice = new HashMap<>();

    if (available && jsonSku.has(internalId)) {
      JSONObject eachPrice = jsonSku.getJSONObject(internalId);

      if (eachPrice.has("finalPrice")) {
        JSONObject finalPrice = eachPrice.getJSONObject("finalPrice");

        if (finalPrice.has("amount")) {
          Float price = CrawlerUtils.getFloatValueFromJSON(finalPrice, "amount");
          installmentPrice.put(1, price);
          prices.setBankTicketPrice(price);
        }
      }

      if (eachPrice.has("oldPrice")) {
        JSONObject oldPrice = eachPrice.getJSONObject("oldPrice");

        if (oldPrice.has("amount")) {
          Double priceFrom = CrawlerUtils.getDoubleValueFromJSON(oldPrice, "amount", false, false);
          prices.setPriceFrom(priceFrom);
        }
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPrice);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPrice);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPrice);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPrice);

    }
    return prices;
  }

  private String crawlSecondaryImages(JSONObject skuJson, String internalId, boolean available) {
    JSONArray secondaryImages = new JSONArray();

    if (available && skuJson.has("jsonConfig")) {
      JSONObject jsonConfig = skuJson.getJSONObject("jsonConfig");
      if (jsonConfig.has("images")) {
        JSONObject images = jsonConfig.getJSONObject("images");

        if (images.has(internalId)) {
          JSONArray image = images.getJSONArray(internalId);

          for (Object object : image) {
            JSONObject img = (JSONObject) object;

            if (img.has("isMain") && !img.getBoolean("isMain") && img.has("img")) {
              secondaryImages.put(img.getString("img"));
            }
          }
        }
      }
    }

    return secondaryImages.toString();
  }

  private String crawlPrimaryImage(JSONObject skuJson, String internalId, boolean available) {
    String primaryImage = null;
    if (available && skuJson.has("jsonConfig")) {
      JSONObject jsonConfig = skuJson.getJSONObject("jsonConfig");
      if (jsonConfig.has("images")) {
        JSONObject images = jsonConfig.getJSONObject("images");

        if (images.has(internalId)) {
          JSONArray image = images.getJSONArray(internalId);

          for (Object object : image) {
            JSONObject img = (JSONObject) object;

            if (img.has("isMain") && img.getBoolean("isMain") && img.has("img")) {
              primaryImage = img.getString("img");
            }
          }
        }
      }
    }
    return primaryImage;
  }

  private String crawlName(Document doc, Map<String, Set<String>> variationsMap, String internalId) {
    Element title = doc.selectFirst("h1 span[itemprop=\"name\"]");
    String name = null;

    if (title != null) {
      name = title.text();

      if (variationsMap.containsKey(internalId)) {
        String variation = variationsMap.get(internalId).toString();

        if (variation.contains("[") && variation.contains("]")) {
          variation = variation.replace("[", "").replace("]", "");
        }

        if (variation.contains("disabled")) {
          variation = variation.replaceAll("disabled", "");
        }
        name = name.concat(" ").concat(variation);
      }

    }
    return name;
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

  private JSONObject crawlJsonHtml(Document doc) {
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
