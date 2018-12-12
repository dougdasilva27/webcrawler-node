package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public class CornershopCrawler {

  private String storeId;
  private Session session;
  private Logger logger;
  private List<Cookie> cookies;

  public CornershopCrawler(Session session, String storeId, Logger logger, List<Cookie> cookies) {
    this.session = session;
    this.storeId = storeId;
    this.logger = logger;
    this.cookies = cookies;
  }

  private static final String PRODUCTS_API_URL = "https://cornershopapp.com/api/v1/branches/";

  public Object fetch() {
    String url = session.getOriginalURL();

    if (url.contains("product/")) {
      String id = CommonMethods.getLast(url.split("product/")).split("//?")[0].trim();

      if (!id.isEmpty()) {
        String urlApi = PRODUCTS_API_URL + storeId + "/products/" + id;

        JSONArray array = new JSONArray(POSTFetcher.requestStringUsingFetcher(urlApi, cookies, null, null, DataFetcher.GET_REQUEST, session, false));

        if (array.length() > 0) {
          return array.getJSONObject(0);
        }
      }
    }

    return new JSONObject();
  }

  public List<Product> extractInformation(JSONObject jsonSku) throws Exception {
    List<Product> products = new ArrayList<>();

    if (jsonSku.length() > 0) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(jsonSku);
      CategoryCollection categories = new CategoryCollection();
      String description = crawlDescription(jsonSku);
      boolean available = crawlAvailability(jsonSku);
      Float price = CrawlerUtils.getFloatValueFromJSON(jsonSku, "price");
      Double priceFrom = CrawlerUtils.getDoubleValueFromJSON(jsonSku, "original_price");
      String primaryImage = crawlPrimaryImage(jsonSku);
      String name = crawlName(jsonSku);
      Prices prices = crawlPrices(price, priceFrom);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setName(name).setPrice(price)
          .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setDescription(description).setMarketplace(new Marketplace())
          .build();

      products.add(product);


    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }

  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("id")) {
      internalId = json.get("id").toString();
    }

    return internalId;
  }


  private String crawlName(JSONObject json) {
    String name = null;

    if (json.has("name")) {
      name = json.getString("name");

      if (json.has("brand")) {
        JSONObject brand = json.getJSONObject("brand");

        if (brand.has("name")) {
          String brandName = brand.get("name").toString().trim();

          if (!brandName.isEmpty()) {
            name = brandName + " · " + name;
          }
        }
      }
    }

    return name;
  }

  private boolean crawlAvailability(JSONObject json) {
    return json.has("availability_status") && json.get("availability_status").toString().equalsIgnoreCase("available");
  }

  private String crawlPrimaryImage(JSONObject json) {
    String primaryImage = null;

    if (json.has("img_url")) {
      primaryImage = json.get("img_url").toString();
    }

    return primaryImage;
  }


  private String crawlDescription(JSONObject json) {
    StringBuilder description = new StringBuilder();

    if (json.has("package") && json.get("package") instanceof String) {
      description.append(json.getString("package"));
      description.append("</br>");
    }


    if (json.has("description") && json.get("description") instanceof String) {
      description.append(json.getString("description"));
    }

    return description.toString();
  }

  private Prices crawlPrices(Float price, Double priceFrom) {
    Prices p = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();
      installmentPriceMap.put(1, price);

      p.setPriceFrom(priceFrom);
      p.setBankTicketPrice(price);

      p.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      p.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      p.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      p.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      p.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      p.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);

    }

    return p;
  }
}
