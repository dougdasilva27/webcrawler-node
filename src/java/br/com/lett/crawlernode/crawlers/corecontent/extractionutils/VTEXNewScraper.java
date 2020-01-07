package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

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
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public abstract class VTEXNewScraper extends Crawler {

  public VTEXNewScraper(Session session) {
    super(session);
  }

  private final String homePage = getHomePage();
  private final List<String> mainSellersNames = getMainSellersNames();
  private final List<Card> cards = getCards();

  protected abstract String getHomePage();

  protected abstract List<String> getMainSellersNames();

  protected abstract List<Card> getCards();

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(homePage));
  }

  @Override
  public List<Product> extractInformation(Document doc) {
    List<Product> products = new ArrayList<>();

    JSONObject runTimeJSON = scrapRuntimeJson(doc);
    JSONObject productJson = scrapProductJson(runTimeJSON);

    if (productJson.has("productId")) {
      String internalPid = productJson.has("productId") && !productJson.isNull("productId") ? productJson.get("productId").toString() : null;
      CategoryCollection categories = scrapCategories(productJson);
      String description = JSONUtils.getStringValue(productJson, "description");

      JSONArray items = productJson.has("items") && !productJson.isNull("items") ? productJson.getJSONArray("items") : new JSONArray();

      for (int i = 0; i < items.length(); i++) {
        JSONObject jsonSku = items.getJSONObject(i);

        Product product = extractProduct(internalPid, categories, description, jsonSku);

        products.add(product);
      }
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
    }

    return products;
  }

  protected Product extractProduct(String internalPid, CategoryCollection categories, String description, JSONObject jsonSku) {

    String internalId = jsonSku.has("itemId") ? jsonSku.get("itemId").toString() : null;
    String name = jsonSku.has("nameComplete") ? jsonSku.get("nameComplete").toString() : null;
    Map<String, Prices> marketplaceMap = scrapMarketplace(jsonSku);
    List<String> mainSellers = CrawlerUtils.getMainSellers(marketplaceMap, this.mainSellersNames);
    Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, mainSellers, this.cards, session);
    boolean available = CrawlerUtils.getAvailabilityFromMarketplaceMap(marketplaceMap, mainSellers);

    List<String> images = scrapImages(jsonSku);
    String primaryImage = !images.isEmpty() ? images.get(0) : null;
    String secondaryImages = scrapSecondaryImages(images);

    Prices prices = CrawlerUtils.getPrices(marketplaceMap, mainSellers);
    Float price = CrawlerUtils.extractPriceFromPrices(prices, this.cards);

    List<String> eans = jsonSku.has("ean") ? Arrays.asList(jsonSku.get("ean").toString()) : null;

    // Creating the product
    return ProductBuilder.create()
        .setUrl(session.getOriginalURL())
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
        .setMarketplace(marketplace)
        .setEans(eans)
        .build();
  }

  private JSONObject scrapRuntimeJson(Document doc) {
    JSONObject runtimeJson = new JSONObject();
    String token = "__RUNTIME__ =";

    Elements scripts = doc.select("script");
    for (Element e : scripts) {
      String script = e.html();

      if (script.contains(token)) {
        runtimeJson = CrawlerUtils.stringToJSONObject(CrawlerUtils.extractSpecificStringFromScript(script, token, false, "", true));
        break;
      }
    }

    return runtimeJson;
  }

  private JSONObject scrapProductJson(JSONObject stateJson) {
    JSONObject product = new JSONObject();
    Object queryData = JSONUtils.getValue(stateJson, "queryData");
    JSONObject queryDataJson = new JSONObject();

    if (queryData instanceof JSONObject) {
      queryDataJson = (JSONObject) queryData;
    } else if (queryData instanceof JSONArray) {
      JSONArray queryDataArray = (JSONArray) queryData;
      if (queryDataArray.length() > 0 && queryDataArray.get(0) instanceof JSONObject) {
        queryDataJson = queryDataArray.getJSONObject(0);
      }
    }

    if (queryDataJson.has("data") && queryDataJson.get("data") instanceof JSONObject) {
      product = queryDataJson.getJSONObject("data");
    } else if (queryDataJson.has("data") && queryDataJson.get("data") instanceof String) {
      product = CrawlerUtils.stringToJson(queryDataJson.getString("data"));
    } else if (queryDataJson.has("data") && queryDataJson.get("data") instanceof String) {
      product = CrawlerUtils.stringToJson(queryDataJson.getString("data"));
    }

    return JSONUtils.getJSONValue(product, "product");
  }

  private CategoryCollection scrapCategories(JSONObject product) {
    CategoryCollection categories = new CategoryCollection();

    JSONArray categoriesArray = JSONUtils.getJSONArrayValue(product, "categories");
    for (int i = categoriesArray.length() - 1; i >= 0; i--) {
      String path = categoriesArray.get(i).toString();

      if (path.contains("/")) {
        categories.add(CommonMethods.getLast(path.split("/")));
      }
    }

    return categories;
  }

  private List<String> scrapImages(JSONObject skuJson) {
    List<String> images = new ArrayList<>();

    for (String key : skuJson.keySet()) {
      if (key.startsWith("images")) {
        JSONArray imagesArray = skuJson.getJSONArray(key);

        for (Object o : imagesArray) {
          JSONObject image = (JSONObject) o;

          if (image.has("imageUrl") && !image.isNull("imageUrl")) {
            images.add(CrawlerUtils.completeUrl(image.get("imageUrl").toString(), "https", "jumbo.vteximg.com.br"));
          }
        }

        break;
      }
    }

    return images;
  }

  private String scrapSecondaryImages(List<String> images) {
    String secondaryImages = null;
    JSONArray imagesArray = new JSONArray();

    if (!images.isEmpty()) {
      images.remove(0);

      for (String image : images) {
        imagesArray.put(image);
      }
    }

    if (imagesArray.length() > 0) {
      secondaryImages = imagesArray.toString();
    }

    return secondaryImages;
  }

  private Map<String, Prices> scrapMarketplace(JSONObject jsonSku) {
    Map<String, Prices> map = new HashMap<>();

    if (jsonSku.has("sellers") && !jsonSku.isNull("sellers")) {
      JSONArray sellers = jsonSku.getJSONArray("sellers");

      for (Object o : sellers) {
        JSONObject seller = (JSONObject) o;

        if (seller.has("sellerName") && !seller.isNull("sellerName") && seller.has("commertialOffer") && !seller.isNull("commertialOffer")) {
          Prices prices = scrapPrices(seller.getJSONObject("commertialOffer"));

          if (!prices.isEmpty()) {
            map.put(seller.get("sellerName").toString(), prices);
          }
        }
      }
    }

    return map;
  }

  private Prices scrapPrices(JSONObject comertial) {
    Prices prices = new Prices();

    if (comertial.has("Price") && !comertial.isNull("Price")) {
      Float price = CrawlerUtils.getFloatValueFromJSON(comertial, "Price", true, false);

      if (price > 0) {
        Map<Integer, Float> installments = new HashMap<>();
        installments.put(1, price);
        prices.setPriceFrom(CrawlerUtils.getDoubleValueFromJSON(comertial, "ListPrice", true, false));

        if (comertial.has("Installments") && !comertial.isNull("Installments")) {
          JSONArray installmentsArray = comertial.getJSONArray("Installments");

          for (Object o : installmentsArray) {
            JSONObject installmentJson = (JSONObject) o;

            Integer installmentNumber = CrawlerUtils.getIntegerValueFromJSON(installmentJson, "NumberOfInstallments", null);
            Float installmentValue = CrawlerUtils.getFloatValueFromJSON(installmentJson, "Value");

            if (installmentNumber != null && installmentValue != null) {
              installments.put(installmentNumber, installmentValue);
            }
          }
        }

        for (Card c : cards) {
          prices.insertCardInstallment(c.toString(), installments);
        }
      }
    }

    return prices;
  }
}
