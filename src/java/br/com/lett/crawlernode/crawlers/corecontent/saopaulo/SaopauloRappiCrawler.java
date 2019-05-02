package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
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
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.Seller;
import models.Util;
import models.prices.Prices;

public class SaopauloRappiCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.rappi.com";
  private static final String STORES_API_URL = "https://services.rappi.com.br/api/base-crack/principal?lat=-23.584&lng=-46.671&device=2";
  public static final String PRODUCTS_API_URL = "https://services.rappi.com.br/chewbacca/search/v2/products";

  public SaopauloRappiCrawler(Session session) {
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
      Float price = available ? crawlPrice(jsonSku) : null;
      Double priceFrom = available ? crawlPriceFrom(jsonSku) : null;
      String primaryImage = crawlPrimaryImage(jsonSku);
      String name = crawlName(jsonSku);
      Prices prices = crawlPrices(price, priceFrom);
      Marketplace marketplace = crawlMarketplace(price, prices, jsonSku);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(productUrl).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrices(new Prices()).setAvailable(false).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setDescription(description).setMarketplace(marketplace).build();

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
    return (url.contains("store_type") && url.contains("query")) || url.contains("product/");
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

  private Double crawlPriceFrom(JSONObject json) {
    Double price = null;

    if (json.has("real_price")) {
      Object pObj = json.get("real_price");

      if (pObj instanceof Double) {
        price = (Double) pObj;
      }
    }

    return price;
  }

  private Float crawlPrice(JSONObject json) {
    Float price = null;

    if (json.has("price")) {
      Object pObj = json.get("price");

      if (pObj instanceof Double) {
        price = MathUtils.normalizeTwoDecimalPlaces(((Double) pObj).floatValue());
      }
    }

    return price;
  }

  private boolean crawlAvailability(JSONObject json) {
    return json.has("is_available") && json.getBoolean("is_available");
  }

  private String crawlPrimaryImage(JSONObject json) {
    String primaryImage = null;

    if (json.has("image")) {
      primaryImage = "https://img.rappi.com.br/products/high/" + json.get("image");
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

  private Marketplace crawlMarketplace(Float price, Prices prices, JSONObject jsonSku) {
    Marketplace marketplace = new Marketplace();

    if (price != null && jsonSku.has("store_name")) {
      String sellerName = jsonSku.get("store_name").toString().replace("null", "").trim();

      if (!sellerName.isEmpty()) {

        try {
          Seller seller = new Seller(new JSONObject().put("price", price).put("prices", prices.toJSON()).put("name", sellerName));
          marketplace.add(seller);
        } catch (Exception e) {
          Logging.printLogError(logger, session, Util.getStackTraceString(e));
        }
      }
    }

    return marketplace;
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
    Map<String, String> stores = crawlStores();

    String storeType = "hiper";
    String storeId = stores.containsKey(storeType) ? stores.get(storeType) : null;
    String productId = null;

    if (productUrl.contains("?")) {
      String[] parameters = (productUrl.split("\\?")[1]).split("&");

      for (String parameter : parameters) {
        if (parameter.contains("store_type=")) {
          storeType = parameter.split("=")[1];

          if (stores.containsKey(storeType)) {
            storeId = stores.get(storeType);
          }

        } else if (parameter.contains("query=")) {
          productId = parameter.split("=")[1];
        }
      }
    } else if (productUrl.contains("_")) {
      productId = CommonMethods.getLast(productUrl.split("_"));
    }

    if (productId != null && storeType != null && storeId != null) {

      String payload = "{\"query\":\"" + productId + "\",\"stores\":[\"" + storeId + "\"]," + "\"helpers\":{\"type\":\"by_products\",\"storeType\":\""
          + storeType + "\"},\"page\":1,\"store_type\":\"" + storeType + "\",\"options\":{}}";

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");

      Request request = RequestBuilder.create().setUrl(PRODUCTS_API_URL + "?page=1").setCookies(cookies).setHeaders(headers).setPayload(payload)
          .mustSendContentEncoding(false).build();
      String page = this.dataFetcher.post(session, request).getBody();

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

  private Map<String, String> crawlStores() {
    Map<String, String> stores = new HashMap<>();
    Request request = RequestBuilder.create().setUrl(STORES_API_URL).setCookies(cookies).build();
    JSONArray options = CrawlerUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());

    for (Object o : options) {
      JSONObject option = (JSONObject) o;

      if (option.has("suboptions")) {
        JSONArray suboptions = option.getJSONArray("suboptions");

        for (Object ob : suboptions) {
          JSONObject suboption = (JSONObject) ob;
          if (suboption.has("stores")) {
            setStores(suboption.getJSONArray("stores"), stores);
          }
        }
      } else if (option.has("stores")) {
        setStores(option.getJSONArray("stores"), stores);
      }
    }

    return stores;
  }

  private void setStores(JSONArray storesArray, Map<String, String> stores) {
    for (Object o : storesArray) {
      JSONObject store = (JSONObject) o;

      if (store.has("store_id") && store.has("store_type")) {
        stores.put(store.getString("store_type"), store.getString("store_id"));
      }
    }
  }
}
