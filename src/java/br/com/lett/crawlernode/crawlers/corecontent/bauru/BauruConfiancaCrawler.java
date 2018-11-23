package br.com.lett.crawlernode.crawlers.corecontent.bauru;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
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

/**
 * date: 12/04/2018
 * 
 * @author gabriel
 *
 */
public class BauruConfiancaCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.confianca.com.br/";

  public BauruConfiancaCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }


  @Override
  public void handleCookiesBeforeFetch() {
    Logging.printLogDebug(logger, session, "Adding cookie...");

    BasicClientCookie cookie = new BasicClientCookie("current_website", "bauru");
    cookie.setDomain("www.confianca.com.br");
    cookie.setPath("/");
    this.cookies.add(cookie);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalPid = crawlInternalPid(doc);
      JSONObject json = crawlProductApi(internalPid);

      String internalId = crawlInternalId(json);
      String name = crawlName(json);
      Integer stock = crawlStock(json);
      boolean available = stock != null && stock > 0;
      Float price = available ? crawlPrice(json) : null;
      Prices prices = crawlPrices(price, json);
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage = crawlPrimaryImage(json);
      String description = crawlDescription(json);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setDescription(description).setStock(stock)
          .setMarketplace(new Marketplace()).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".main-container product-essential") != null;
  }

  private JSONObject crawlProductApi(String internalPid) {
    JSONObject json = new JSONObject();

    if (internalPid != null) {
      Map<String, String> headers = new HashMap<>();
      headers.put("Referer", session.getOriginalURL());

      String url = "https://www.confianca.com.br/bizrest/action/product/id/" + internalPid;
      json = new JSONObject(POSTFetcher.requestUsingFetcher(url, cookies, headers, null, DataFetcher.GET_REQUEST, session, false));
    }

    return json;
  }

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("sku")) {
      internalId = json.getString("sku");
    }

    return internalId;
  }

  private String crawlInternalPid(Document doc) {
    String internalPid = null;

    Element pid = doc.selectFirst(".main-container product-essential");
    if (pid != null) {
      internalPid = pid.attr(":id");
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

  private Integer crawlStock(JSONObject json) {
    Integer stock = null;

    if (json.has("max_sale_quantity")) {
      Object stc = json.get("max_sale_quantity");

      if (stc instanceof Integer) {
        stock = (Integer) stc;
      }
    }

    return stock;
  }

  private Float crawlPrice(JSONObject json) {
    Float price = null;

    if (json.has("final_price")) {
      price = CrawlerUtils.getFloatValueFromJSON(json, "final_price");
    }

    return price;
  }

  private String crawlPrimaryImage(JSONObject json) {
    String primaryImage = null;

    if (json.has("image")) {
      primaryImage = json.get("image").toString();
    }

    return primaryImage;
  }

  /**
   * @param document
   * @return
   */
  private CategoryCollection crawlCategories(Document doc) {
    CategoryCollection categories = new CategoryCollection();

    Element categoriesElement = doc.selectFirst(".main-container product-essential");
    if (categoriesElement != null) {
      JSONArray array = CrawlerUtils.stringToJsonArray(categoriesElement.attr(":breadcrumb"));

      for (Object o : array) {
        JSONObject json = (JSONObject) o;

        if (json.has("name") && !json.get("name").toString().trim().equalsIgnoreCase("home")) {
          categories.add(json.get("name").toString());
        }
      }
    }

    return categories;
  }

  private String crawlDescription(JSONObject json) {
    StringBuilder description = new StringBuilder();

    if (json.has("short_description")) {
      description.append(json.get("short_description"));
    }

    if (json.has("description")) {
      description.append(json.get("description"));
    }

    return description.toString();
  }

  /**
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, JSONObject jsonSku) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);

      Float priceOld = CrawlerUtils.getFloatValueFromJSON(jsonSku, "price_old");
      if (!price.equals(priceOld)) {
        prices.setPriceFrom(MathUtils.normalizeTwoDecimalPlaces(priceOld.doubleValue()));
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
    }

    return prices;
  }
}
