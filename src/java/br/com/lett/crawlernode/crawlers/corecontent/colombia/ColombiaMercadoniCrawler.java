package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import com.google.gson.JsonParser;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
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

public class ColombiaMercadoniCrawler extends Crawler {

  private static final String LOCATION = "557b4c374e1d3b1f00793e12";
  private static final String HOME_PAGE = "https://www.mercadoni.com.co/";
  public static final String PRODUCTS_API_URL = "https://j9xfhdwtje-3.algolianet.com/1/indexes/live_products_boost_desc/query"
      + "?x-algolia-agent=Algolia%20for%20vanilla%20JavaScript%203.30.0&x-algolia-application-id=J9XFHDWTJE"
      + "&x-algolia-api-key=2065b01208843995dbf34b4c58e8b7be";

  public ColombiaMercadoniCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  protected Object fetch() {
    return crawlProductInformatioFromApi(session.getOriginalURL());
  }

  @Override
  public List<Product> extractInformation(JSONObject skuJson) throws Exception {
    super.extractInformation(skuJson);
    List<Product> products = new ArrayList<>();

    if (isProductPage(session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      JSONArray arraySkus = skuJson != null && skuJson.has("hits") ? skuJson.getJSONArray("hits") : new JSONArray();

      for (Object o : arraySkus) {
        JSONObject jsonSku = (JSONObject) o;
        String internalId = crawlInternalId(jsonSku);
        String internalPid = crawlInternalPid(jsonSku);
        CategoryCollection categories = crawlCategories(jsonSku);
        String description = crawlDescription(jsonSku);
        Integer stock = jsonSku.has("stock") && jsonSku.get("stock") instanceof Integer ? jsonSku.getInt("stock") : null;
        boolean available = stock != null && stock > 0;
        Float price = available ? CrawlerUtils.getFloatValueFromJSON(jsonSku, "price") : null;
        Double priceFrom = available ? CrawlerUtils.getDoubleValueFromJSON(jsonSku, "special_price") : null;
        String primaryImage = crawlPrimaryImage(jsonSku);
        String name = crawlName(jsonSku);
        Prices prices = crawlPrices(price, priceFrom);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setDescription(description).setMarketplace(new Marketplace())
            .setStock(stock).build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(String url) {
    return url.contains("product_simple");
  }

  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("product_simple")) {
      internalId = json.get("product_simple").toString();
    }

    return internalId;
  }


  private String crawlInternalPid(JSONObject json) {
    String internalPid = null;

    if (json.has("retailer_sku")) {
      internalPid = json.getString("retailer_sku");
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

  private String crawlPrimaryImage(JSONObject json) {
    String primaryImage = null;

    if (json.has("image_url")) {
      primaryImage = CrawlerUtils.completeUrl(json.get("image_url").toString(), "https:", "catalog.images.mercadoni.com");
    }

    return primaryImage;
  }

  private CategoryCollection crawlCategories(JSONObject json) {
    CategoryCollection categories = new CategoryCollection();

    if (json.has("categories")) {
      JSONArray shelfList = json.getJSONArray("categories");

      for (Object o : shelfList) {
        JSONObject cat = (JSONObject) o;

        if (cat.has("name")) {
          categories.add(cat.getString("name"));
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
   * https://www.mercadoni.com.co/tienda/jumbo-colombia/p/Woolite-Detergente-L%C3%ADquido-Todos-Los-Dias-2000Ml-X-2-Bi-Pack?retailer_sku=75009842549
   * 
   * @return
   */
  private JSONObject crawlProductInformatioFromApi(String productUrl) {
    JSONObject products = new JSONObject();

    String productId = null;

    if (productUrl.contains("?") && productUrl.contains("&")) {
      String parametersUrl = productUrl.split("\\?")[1];

      String[] parameters = parametersUrl.split("&");

      for (String parameter : parameters) {
        if (parameter.contains("product_simple=")) {
          productId = parameter.split("=")[1];
        }
      }
    }

    if (productId != null) {
      String payload = "{\"params\":\"query=&hitsPerPage=1&page=0&facets=&facetFilters=%5B%5B%22product_simple%3A%20" + productId
          + "%22%5D%2C%5B%5D%2C%22active%3A%20true%22%2C%22location%3A%20" + LOCATION + "%22%2C%22product_simple_active"
          + "%3A%20true%22%2C%22visible%3A%20true%22%5D&numericFilters=%5B%22stock%3E0%22%5D&typoTolerance=strict"
          + "&restrictSearchableAttributes=%5B%22name%22%5D\"}";

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");

      String page =
          POSTFetcher.fetchPagePOSTWithHeaders(PRODUCTS_API_URL, session, payload, cookies, 1, headers, DataFetcher.randUserAgent(), null).trim();

      if (page.startsWith("{") && page.endsWith("}")) {
        try {
          // Using google JsonObject to get a JSONObject because this json can have a duplicate key.
          products = new JSONObject(new JsonParser().parse(page).getAsJsonObject().toString());
        } catch (Exception e) {
          Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
        }
      }
    }

    return products;
  }
}
