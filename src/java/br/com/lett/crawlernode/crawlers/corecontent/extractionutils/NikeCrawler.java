package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.prices.Prices;

public class NikeCrawler extends Crawler {

  // Must be changed for each child (default: USA)
  protected static final String HOME_PAGE = "https://www.nike.com";
  protected static final String COUNTRY_URL = "/us/en_us/";

  protected final Map<String, String> defaultHeaders;

  public NikeCrawler(Session session) {
    super(session);

    defaultHeaders = new HashMap<>();
    defaultHeaders.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
    defaultHeaders.put("accept-encoding", "gzip, deflate, br");
    defaultHeaders.put("accept-language", "en-US,en;q=0.9");
    defaultHeaders.put("cache-control", "max-age=0");
    defaultHeaders.put("upgrade-insecure-requests", "1");
    defaultHeaders.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36");
  }

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  // @Override
  // public void handleCookiesBeforeFetch() { // cookies =
  // CrawlerUtils.fetchCookiesFromAPage(HOME_PAGE + COUNTRY_URL, null, ".nike.com", "/", null,
  // session, defaultHeaders);
  // }

  @Override
  protected Object fetch() {
    Request request = RequestBuilder.create().setUrl(session.getOriginalURL()).setCookies(cookies).setHeaders(defaultHeaders).build();
    return this.dataFetcher.get(session, request).getBody();
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.INITIAL_REDUX_STATE=", ";", false, true);
      json = json.has("Threads") ? json.getJSONObject("Threads") : new JSONObject();
      json = json.has("products") ? json.getJSONObject("products") : new JSONObject();

      for (int i = 0; i < json.names().length(); i++) {
        JSONObject internalProduct = json.getJSONObject(json.names().getString(i));

        String internalPid = internalProduct.has("pid") ? internalProduct.getString("pid") : null;
        String name = internalProduct.has("fullTitle") ? internalProduct.getString("fullTitle") : null;
        String description = getDescription(internalProduct);
        String primaryImage = internalProduct.has("firstImageUrl") ? internalProduct.getString("firstImageUrl") : null;
        String secondaryImages = getSecondaryImages(internalProduct);
        Float price = internalProduct.has("currentPrice") ? internalProduct.getFloat("currentPrice") : null;
        Prices prices = getPrices(internalProduct, price);

        JSONArray availableSkus = internalProduct.has("availableSkus") ? internalProduct.getJSONArray("availableSkus") : new JSONArray();

        JSONArray skus = internalProduct.has("skus") ? internalProduct.getJSONArray("skus") : new JSONArray();
        for (Object o : skus) {
          JSONObject sku = (JSONObject) o;

          if (sku.has("id")) {
            String skuName = getSkuName(sku, name);
            String internalId = sku.getString("id");
            String skuId = sku.has("skuId") ? sku.getString("skuId") : null;
            boolean available = getAvailability(availableSkus, skuId);

            // Creating the product
            Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid)
                .setName(skuName).setPrice(price).setPrices(prices).setDescription(description).setPrimaryImage(primaryImage)
                .setSecondaryImages(secondaryImages).setAvailable(available).build();

            products.add(product);
          }
        }
      }
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  protected boolean isProductPage(Document doc) {
    return doc.selectFirst(".visual-search-product-col") != null;
  }

  protected String getDescription(JSONObject json) {
    StringBuilder sb = new StringBuilder();

    if (json.has("descriptionPreview")) {
      sb.append(json.getString("descriptionPreview"));
      sb.append("<br>");
    }

    if (json.has("description")) {
      sb.append(json.getString("description"));
    }

    return sb.toString();
  }

  protected String getSkuName(JSONObject json, String name) {
    if (json.has("nikeSize")) {
      String size = json.getString("nikeSize");

      if (!size.isEmpty()) {
        name += " - " + size;
      }
    }

    return name;
  }

  protected Prices getPrices(JSONObject json, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Double priceFrom = json.has("fullPrice") ? json.getDouble("fullPrice") : null;
      prices.setPriceFrom(priceFrom);

      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DISCOVER.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);
    }

    return prices;
  }

  protected String getSecondaryImages(JSONObject json) {
    JSONArray secondaryImages = new JSONArray();
    JSONArray array = json.has("nodes") ? json.getJSONArray("nodes") : new JSONArray();

    if (array.length() > 0) {
      json = array.getJSONObject(0);
      array = json.has("nodes") ? json.getJSONArray("nodes") : new JSONArray();

      // jump the first element since we aready have the primary image
      for (int i = 1; i < array.length(); i++) {
        JSONObject imageJson = array.getJSONObject(i);

        // check if propertie is a image
        if (imageJson.has("subType") && imageJson.getString("subType").equals("image") && imageJson.has("properties")) {
          imageJson = imageJson.getJSONObject("properties");

          if (imageJson.has("portraitURL")) {
            secondaryImages.put(imageJson.getString("portraitURL"));
          }
        }
      }
    }

    if (secondaryImages.length() > 0) {
      return secondaryImages.toString();
    }

    return null;
  }

  protected boolean getAvailability(JSONArray availabilityArray, String skuId) {
    boolean available = false;

    if (skuId != null) {
      for (Object o : availabilityArray) {
        JSONObject sku = (JSONObject) o;

        // found the id
        if (sku.has("skuId") && sku.getString("skuId").equals(skuId) && sku.has("available")) {
          available = sku.getBoolean("available");
          break;
        }
      }
    }

    return available;
  }

}
