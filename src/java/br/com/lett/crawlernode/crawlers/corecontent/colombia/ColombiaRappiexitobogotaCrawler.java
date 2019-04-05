package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public class ColombiaRappiexitobogotaCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.rappi.com.co/";
  public static final String PRODUCTS_API_URL = "https://services.grability.rappi.com/api/search-client/search/v2/products";
  public static final List<String> STORES = Arrays.asList("6660303");

  public ColombiaRappiexitobogotaCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  protected Object fetch() {
    return new Document("");
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String productUrl = session.getOriginalURL();
      JSONObject jsonSku = crawlProductInformatioFromApi(productUrl);
      String internalId = crawlInternalId(jsonSku);
      String internalPid = crawlInternalPid(jsonSku);
      CategoryCollection categories = crawlCategories(jsonSku);
      String description = crawlDescription(jsonSku);
      boolean available = crawlAvailability(jsonSku);
      Float price = available ? CrawlerUtils.getFloatValueFromJSON(jsonSku, "price") : null;
      Double priceFrom = available ? CrawlerUtils.getDoubleValueFromJSON(jsonSku, "real_price") : null;
      String primaryImage = crawlPrimaryImage(jsonSku);
      String name = crawlName(jsonSku);
      Prices prices = crawlPrices(price, priceFrom);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(productUrl).setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
          .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setDescription(description).setMarketplace(new Marketplace())
          .build();

      products.add(product);


    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(String url) {
    return url.contains("store_type") && url.contains("query");
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


  private String crawlInternalPid(JSONObject json) {
    String internalPid = null;

    if (json.has("product_id")) {
      internalPid = json.getString("product_id");
    }

    return internalPid;
  }

  private String crawlName(JSONObject json) {
    String name = null;

    if (json.has("name")) {
      name = json.getString("name");
    }

    return name;
  }

  private boolean crawlAvailability(JSONObject json) {
    return json.has("is_available") && json.getBoolean("is_available");
  }

  private String crawlPrimaryImage(JSONObject json) {
    String primaryImage = null;

    if (json.has("image")) {
      primaryImage = "https://images.rappi.com/products/" + json.get("image");
    }

    return primaryImage;
  }

  private CategoryCollection crawlCategories(JSONObject json) {
    CategoryCollection categories = new CategoryCollection();

    if (json.has("categories")) {
      JSONArray shelfList = json.getJSONArray("categories");

      for (Object o : shelfList) {
        JSONObject cat = (JSONObject) o;

        if (cat.has("category_name")) {
          categories.add(cat.getString("category_name"));
        }
      }
    }

    return categories;
  }


  private String crawlDescription(JSONObject json) {
    StringBuilder description = new StringBuilder();

    if (json.has("description") && json.get("description") instanceof String) {
      description.append(json.getString("description"));
    }


    return description.toString();
  }

  /**
   * In this site has no information of installments
   * 
   * @param price
   * @return
   */
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


  /**
   * - Get the json of api, this api has all info of product - Spected url like this
   * https://www.rappi.com.br/search?store_type=market&query=2089952206
   * 
   * @return
   */
  private JSONObject crawlProductInformatioFromApi(String productUrl) {
    JSONObject productsInfo = new JSONObject();

    String storeType = null;
    String productId = null;

    if (productUrl.contains("?")) {
      String[] parameters = (productUrl.split("\\?")[1]).split("&");

      for (String parameter : parameters) {
        if (parameter.contains("store_type=")) {
          storeType = parameter.split("=")[1];

        } else if (parameter.contains("query=")) {
          productId = parameter.split("=")[1];
        }
      }
    }

    if (productId != null && storeType != null) {

      String payload = "{\"query\":\"" + productId + "\",\"stores\":" + STORES.toString() + ",\"store_type\":\"" + storeType + "\",\"page\":1"
          + ",\"size\":40,\"options\":{},\"helpers\":{\"home_type\":\"by_categories\",\"store_type_group\":\"market\",\"type\":\"by_categories\"}}";

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");

      Request request = RequestBuilder.create().setUrl(PRODUCTS_API_URL + "?page=1").setCookies(cookies).setHeaders(headers).setPayload(payload)
          .mustSendContentEncoding(false).build();
      String page = this.dataFetcher.post(session, request).getBody().trim();

      if (page.startsWith("{") && page.endsWith("}")) {
        try {
          JSONObject apiResponse = new JSONObject(page);

          if (apiResponse.has("hits") && apiResponse.get("hits") instanceof JSONArray) {
            JSONArray hits = apiResponse.getJSONArray("hits");

            if (hits.length() > 0) {
              productsInfo = hits.getJSONObject(0);
            }
          }
        } catch (Exception e) {
          Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
        }
      }
    }

    return productsInfo;
  }
}
