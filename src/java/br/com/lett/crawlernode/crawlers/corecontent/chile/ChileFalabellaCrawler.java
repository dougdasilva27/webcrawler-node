package br.com.lett.crawlernode.crawlers.corecontent.chile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.test.Test;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 22/10/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class ChileFalabellaCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.falabella.com/";
  private static final String IMAGE_URL_FIRST_PART = "https://falabella.scene7.com/is/image/";

  public ChileFalabellaCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      JSONObject productJson = extractProductJson(doc);

      CommonMethods.saveDataToAFile(doc, Test.pathWrite + "FALABELLA.html");

      String internalPid = crawlInternalPid(productJson);
      CategoryCollection categories = crawlCategories(doc);
      String description = crawlDescription(doc);

      JSONArray arraySkus = productJson.has("skus") ? productJson.getJSONArray("skus") : new JSONArray();

      Map<String, List<String>> colorsMap = new HashMap<>();

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject skuJson = arraySkus.getJSONObject(i);

        String internalId = crawlInternalId(skuJson);
        String name = crawlName(productJson, skuJson);
        Integer stock = crawlStock(skuJson);
        boolean available = stock != null && stock > 0;

        JSONObject pricesJson = fetchPrices(internalId, available);
        Prices prices = available ? crawlPrices(skuJson, pricesJson) : new Prices();
        Float price = CrawlerUtils.extractPriceFromPrices(prices, Card.AMEX);

        List<String> images = crawlImages(skuJson, internalId, colorsMap);
        String primaryImage = images.isEmpty() ? null : images.get(0);
        String secondaryImages = crawlSecondaryImages(images, primaryImage);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
            .setStock(stock).setMarketplace(new Marketplace()).build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }

  /**
   * 
   * @param doc
   * @return
   */
  private boolean isProductPage(Document doc) {
    return !doc.select(".fb-product__form").isEmpty();
  }

  /**
   * 
   * @param doc
   * @return
   */
  private JSONObject extractProductJson(Document doc) {
    JSONObject productJson = new JSONObject();

    JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "var fbra_browseMainProductConfig =", "};", false, false);

    if (json.has("state")) {
      JSONObject state = json.getJSONObject("state");

      if (state.has("product")) {
        productJson = state.getJSONObject("product");
      }
    }

    return productJson;
  }

  private CategoryCollection crawlCategories(Document doc) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = doc.select(".fb-masthead__breadcrumb__links > span a");

    for (Element e : elementCategories) {
      categories.add(e.text().replace("/", "").trim());
    }

    return categories;
  }

  /**
   * @param skuJson
   * @return
   */
  private Integer crawlStock(JSONObject skuJson) {
    Integer stock = null;

    if (skuJson.has("onlineStock")) {
      stock = skuJson.getInt("onlineStock");
    }

    return stock;
  }

  private String crawlInternalId(JSONObject skuJson) {
    String internalId = null;

    if (skuJson.has("skuId")) {
      internalId = skuJson.getString("skuId");
    }

    return internalId;
  }


  private String crawlInternalPid(JSONObject productJson) {
    String internalPid = null;

    if (productJson.has("id")) {
      internalPid = productJson.get("id").toString();
    }

    return internalPid;
  }

  private String crawlName(JSONObject productJson, JSONObject skuJson) {
    StringBuilder name = new StringBuilder();

    if (productJson.has("displayName")) {
      name.append(productJson.get("displayName"));

      if (skuJson.has("size")) {
        name.append(" ").append(skuJson.get("size"));
      }

      if (skuJson.has("color")) {
        name.append(" ").append(skuJson.get("color"));
      }
    }

    return name.toString();
  }

  private String crawlSecondaryImages(List<String> images, String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    for (String image : images) {
      if (!image.equalsIgnoreCase(primaryImage)) {
        secondaryImagesArray.put(image);
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private List<String> crawlImages(JSONObject skuJson, String internalId, Map<String, List<String>> colorsMap) {
    List<String> images = new ArrayList<>();

    String colorName = "normal";

    if (skuJson.has("color")) {
      colorName = skuJson.get("color").toString();
    }

    if (colorsMap.containsKey(colorName)) {
      images = colorsMap.get(colorName);
    } else {
      String url = IMAGE_URL_FIRST_PART + "Falabella/" + internalId + "?req=set,json";

      String response = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, cookies);

      JSONObject json = CrawlerUtils.stringToJson(CrawlerUtils.extractSpecificStringFromScript(response, "esponse(", ",", true));

      if (json.has("set")) {
        JSONObject set = json.getJSONObject("set");

        if (set.has("item")) {
          JSONArray items = set.getJSONArray("item");

          for (Object o : items) {
            JSONObject item = (JSONObject) o;

            JSONObject imageJson = new JSONObject();

            if (item.has("s")) {
              imageJson = item.getJSONObject("s");
            } else if (item.has("i")) {
              imageJson = item.getJSONObject("i");
            }

            if (imageJson.has("n")) {
              images.add(IMAGE_URL_FIRST_PART + imageJson.get("n") + "?wid=1080&fmt=jpg");
            }
          }

          colorsMap.put(colorName, images);
        }
      }
    }

    return images;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Elements descriptions = doc.select("[data-panel=longDescription]");
    for (Element e : descriptions) {
      description.append(e.html());
    }

    return description.toString();
  }

  private Prices crawlPrices(JSONObject skuJson, JSONObject jsonPrices) {
    Prices prices = new Prices();

    if (skuJson.has("price")) {
      Map<Integer, Float> mapInstallments = new HashMap<>();
      JSONArray arrayPrices = skuJson.getJSONArray("price");

      for (Object o : arrayPrices) {
        JSONObject priceJson = (JSONObject) o;

        if (priceJson.has("originalPrice")) {
          Float price = MathUtils.parseFloatWithComma(priceJson.get("originalPrice").toString());

          if (price != null) {

            if (priceJson.has("label")) {
              String label = priceJson.get("label").toString().toLowerCase().trim();

              if (label.isEmpty()) {
                prices.setPriceFrom(MathUtils.normalizeTwoDecimalPlaces(price.doubleValue()));
              } else if (label.contains("oferta")) {
                mapInstallments.put(1, price);
              }
            } else if (mapInstallments.isEmpty()) {
              mapInstallments.put(1, price);
            }
          }
        }
      }

      if (mapInstallments.isEmpty() && prices.getPriceFrom() != null) {
        mapInstallments.put(1, MathUtils.normalizeTwoDecimalPlaces(prices.getPriceFrom().floatValue()));
        prices.setPriceFrom(null);
      }

      if (jsonPrices.has("selectedInstallment") && jsonPrices.has("formattedInstallmentAmount")) {
        String installment = jsonPrices.get("selectedInstallment").toString().replaceAll("[^0-9]", "");
        Float value = MathUtils.parseFloatWithComma(jsonPrices.get("formattedInstallmentAmount").toString());

        if (!installment.isEmpty() && value != null) {
          mapInstallments.put(Integer.parseInt(installment), value);
        }
      }

      prices.insertCardInstallment(Card.AMEX.toString(), mapInstallments);
    }

    return prices;

  }

  public JSONObject fetchPrices(String internalId, boolean available) {
    JSONObject jsonPrice = new JSONObject();

    if (available) {
      String url = "https://www.falabella.com/rest/model/falabella/rest/browse/BrowseActor/init-monthly-installment?"
          + "%7B%22skus%22%3A%5B%7B%22skuId%22%3A%22" + internalId + "%22%2C%22quantity%22%3A1%7D%5D%2C%22installmentNum%22%3A10%7D";

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");

      JSONObject json =
          CrawlerUtils.stringToJson(POSTFetcher.requestUsingFetcher(url, cookies, headers, null, DataFetcher.GET_REQUEST, session, false));

      if (json.has("state")) {
        jsonPrice = json.getJSONObject("state");
      }
    }

    return jsonPrice;
  }
}
