package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.Fetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import models.prices.Prices;

public class BrasilPassarelaCrawler extends Crawler {
  private static final String HOME_PAGE = "https://www.passarela.com.br/";
  private String apiUrl = null;

  public BrasilPassarelaCrawler(Session session) {
    super(session);

    transformUrl(session.getOriginalURL());
  }

  /**
   * Method to tranform the product url into the api url
   * 
   * @param oldUrl
   */
  private void transformUrl(String oldUrl) {
    String urlEnding = oldUrl.substring(oldUrl.indexOf(HOME_PAGE) + HOME_PAGE.length());
    apiUrl = HOME_PAGE + "ccstoreui/v1/pages/" + urlEnding + "&dataOnly=false&cacheableDataOnly=true&productTypesRequired=true";
  }

  @Override
  protected Object fetch() {
    String html = "";
    if (config.getFetcher() == Fetcher.STATIC) {
      html = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, apiUrl, null, cookies);
    } else {
      webdriver = DynamicDataFetcher.fetchPageWebdriver(apiUrl, session);

      if (webdriver != null) {
        html = webdriver.getCurrentPageSource();
      }
    }

    return Jsoup.parse(html);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    JSONObject json = new JSONObject(doc.text());
    json = json.has("data") ? json.getJSONObject("data") : new JSONObject();
    json = json.has("page") ? json.getJSONObject("page") : new JSONObject();

    if (isProductPage(json)) {
      json = json.getJSONObject("product");

      String internalPid = json.has("id") ? json.getString("id") : null;
      String name = json.has("displayName") ? json.getString("displayName") : null;
      String description = getDescription(json);

      Map<String, Integer> stocks = getStocks(internalPid);

      if (json.has("childSKUs")) {
        JSONArray variations = json.getJSONArray("childSKUs");
        Map<String, String> primaryImagesByVariation = new HashMap<>();
        Map<String, String> secondaryImagesByVariation = new HashMap<>();

        // Getting images by variation
        for (Object o : variations) {
          JSONObject sku = (JSONObject) o;
          JSONArray images = getImages(sku);
          String variationName = sku.has("cor") ? sku.getString("cor") : null;

          if (variationName != null) {
            if (images.length() > 0) {
              primaryImagesByVariation.put(variationName, images.getString(0));
              images.remove(0);
            }

            if (images.length() > 0) {
              secondaryImagesByVariation.put(variationName, images.toString());
            }
          }
        }

        // Creating products
        for (Object o : variations) {
          JSONObject sku = (JSONObject) o;

          String internalId = sku.has("repositoryId") ? sku.getString("repositoryId") : null;
          Float price = getPrice(sku);
          String variationName = sku.has("cor") ? sku.getString("cor") : null;
          String primaryImage = null;
          String secondaryImages = null;
          Prices prices = getPrices(sku, price);
          String productName = name + " - " + variationName;
          Integer stock = stocks.get(internalId);
          boolean available = stock > 0;

          if (sku.has("tamanho")) {
            productName += " " + sku.getString("tamanho");
          }

          if (variationName != null) {
            primaryImage = primaryImagesByVariation.get(variationName);
            secondaryImages = secondaryImagesByVariation.get(variationName);
          }

          // Creating the product
          Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid)
              .setName(productName).setDescription(description).setPrice(price).setPrices(prices).setPrimaryImage(primaryImage)
              .setSecondaryImages(secondaryImages).setStock(stock).setAvailable(available).build();

          products.add(product);
        }
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  protected boolean isProductPage(JSONObject json) {
    return json.has("product");
  }

  protected String getDescription(JSONObject json) {
    StringBuilder description = new StringBuilder();

    if (json.has("longDescription")) {
      description.append(json.getString("longDescription"));
      description.append("<br>");
    }

    if (json.has("description")) {
      description.append(json.getString("description"));
      description.append("<br>");
    }

    if (json.has("tabelaDeTamanhos")) {
      description.append(json.getString("tabelaDeTamanhos"));
      description.append("<br>");
    }

    return description.toString();
  }

  protected JSONArray getImages(JSONObject json) {
    JSONArray images = new JSONArray();

    if (json.has("fullImageURLs")) {
      JSONArray subArray = json.getJSONArray("fullImageURLs");
      for (Object o : subArray) {
        String str = (String) o;

        images.put(HOME_PAGE.substring(0, HOME_PAGE.length() - 1) + str);
      }
    } else if (json.has("sourceImageURLs")) {
      JSONArray subArray = json.getJSONArray("sourceImageURLs");
      for (Object o : subArray) {
        String str = (String) o;

        images.put(HOME_PAGE.substring(0, HOME_PAGE.length() - 1) + str);
      }
    } else if (json.has("largeImageURLs")) {
      JSONArray subArray = json.getJSONArray("largeImageURLs");
      for (Object o : subArray) {
        String str = (String) o;

        images.put(HOME_PAGE.substring(0, HOME_PAGE.length() - 1) + str);
      }
    } else if (json.has("mediumImageURLs")) {
      JSONArray subArray = json.getJSONArray("mediumImageURLs");
      for (Object o : subArray) {
        String str = (String) o;

        images.put(HOME_PAGE.substring(0, HOME_PAGE.length() - 1) + str);
      }
    }

    return images;
  }

  protected Float getPrice(JSONObject json) {
    Float price = null;
    String salePrice = "salePrice";

    if (json.has(salePrice)) {
      if (!json.isNull(salePrice)) {
        price = json.getFloat(salePrice);
      } else {
        price = json.has("listPrice") ? json.getFloat("listPrice") : null;
      }
    }


    return price;
  }

  protected Prices getPrices(JSONObject json, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);

      prices.setBankTicketPrice(price);

      if (json.has("listPrice")) {
        prices.setPriceFrom(json.getDouble("listPrice"));
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
    }

    return prices;
  }

  protected Map<String, Integer> getStocks(String productPid) {
    Map<String, Integer> stocks = new HashMap<>();
    Document doc = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session,
        "https://www.passarela.com.br/ccstoreui/v1/stockStatus/?products=" + productPid, null, cookies);

    JSONObject json = new JSONObject(doc.text());

    if (json.has("items")) {
      JSONArray items = json.getJSONArray("items");

      if (items.length() > 0) {
        json = (JSONObject) items.get(0);
        json = json.has("productSkuInventoryStatus") ? json.getJSONObject("productSkuInventoryStatus") : new JSONObject();

        for (int i = 0; i < json.names().length(); i++) {
          stocks.put(json.names().getString(i), json.getInt(json.names().getString(i)));
        }
      }
    }

    return stocks;
  }
}
