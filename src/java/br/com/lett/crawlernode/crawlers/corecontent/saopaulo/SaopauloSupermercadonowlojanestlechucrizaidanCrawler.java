package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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

public class SaopauloSupermercadonowlojanestlechucrizaidanCrawler extends Crawler {

  private static final String HOME_PAGE = "https://supermercadonow.com/";
  private Map<String, String> headers = new HashMap<>();


  public SaopauloSupermercadonowlojanestlechucrizaidanCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  protected Object fetch() {
    Document doc = (Document) super.fetch();

    headers.put("Accept", "application/json, text/plain, */*");
    headers.put("X-SNW-TOKEN", scrapToken(doc));

    String[] parameters = session.getOriginalURL().split("\\?")[0].split("/");
    String pathUrl = CommonMethods.getLast(parameters);
    String store = "";

    for (String parameter : parameters) {
      if (parameter.startsWith("loja-")) {
        store = parameter;
        break;
      }
    }

    String apiUrl = "https://supermercadonow.com/api/v2/stores/" + (store.isEmpty() ? "" : store + "/") + "product/" + pathUrl;

    Request request = RequestBuilder.create().setUrl(apiUrl).setCookies(cookies).setHeaders(headers).build();
    return CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
  }

  private String scrapToken(Document doc) {
    String token = null;
    String identifier = "AuthenticateUser.setToken('";

    Elements scripts = doc.select("script");

    for (Element e : scripts) {
      String script = e.html().replace(" ", "");
      if (script.contains(identifier)) {
        token = CrawlerUtils.extractSpecificStringFromScript(script, "AuthenticateUser.setToken('", "')", false);
        break;
      }
    }

    return token;
  }

  @Override
  public List<Product> extractInformation(JSONObject jsonSku) throws Exception {
    super.extractInformation(jsonSku);

    List<Product> products = new ArrayList<>();

    if (jsonSku.length() > 0) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(jsonSku);
      String internalPid = crawlInternalPid(jsonSku);
      CategoryCollection categories = crawlCategories(internalId);
      String description = crawlDescription(jsonSku);
      boolean available = crawlAvailability(jsonSku);
      Float price = available ? CrawlerUtils.getFloatValueFromJSON(jsonSku, "price", true, false) : null;
      Double priceFrom = available ? CrawlerUtils.getDoubleValueFromJSON(jsonSku, "original_price", true, false) : null;
      String primaryImage = crawlPrimaryImage(jsonSku);
      String name = crawlName(jsonSku);
      String secondaryImages = crawlSecondaryImages(jsonSku, primaryImage);
      Prices prices = crawlPrices(price, priceFrom);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setMarketplace(new Marketplace()).build();

      products.add(product);


    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("product_store_id")) {
      internalId = json.get("product_store_id").toString();
    }

    return internalId;
  }


  private String crawlInternalPid(JSONObject json) {
    String internalPid = null;

    if (json.has("slug")) {
      internalPid = json.get("slug").toString().split("-")[0];
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
    return json.has("in_stock") && json.get("in_stock") instanceof Boolean && json.getBoolean("in_stock");
  }

  private String crawlPrimaryImage(JSONObject json) {
    String primaryImage = null;

    if (json.has("all_images")) {
      JSONArray images = json.getJSONArray("all_images");

      if (images.length() > 0) {
        primaryImage = CrawlerUtils.completeUrl(images.get(0).toString(), "https", "d3o3bdzeq5san1.cloudfront.net");
      }
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(JSONObject json, String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    if (json.has("all_images")) {
      JSONArray images = json.getJSONArray("all_images");

      for (Object o : images) {
        String image = CrawlerUtils.completeUrl(o.toString(), "https", "d3o3bdzeq5san1.cloudfront.net");

        if (!image.equalsIgnoreCase(primaryImage)) {
          secondaryImagesArray.put(image);
        }
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private CategoryCollection crawlCategories(String internalId) {
    CategoryCollection categories = new CategoryCollection();

    String categoryUrl = "https://supermercadonow.com/api/products/" + internalId + "/category-tree";
    Request request = RequestBuilder.create().setUrl(categoryUrl).setCookies(cookies).setHeaders(headers).build();

    JSONArray categoriesArray = CrawlerUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());
    for (Object o : categoriesArray) {
      JSONObject categoryObject = (JSONObject) o;

      if (categoryObject.has("name")) {
        String categoryName = categoryObject.get("name").toString();

        if (!categoryName.equalsIgnoreCase("todos os produtos")) {
          categories.add(categoryName);
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
}
