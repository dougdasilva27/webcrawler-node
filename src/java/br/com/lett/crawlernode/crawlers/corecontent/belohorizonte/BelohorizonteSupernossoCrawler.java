package br.com.lett.crawlernode.crawlers.corecontent.belohorizonte;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
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
 * Date: 01/09/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BelohorizonteSupernossoCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.supernossoemcasa.com.br/e-commerce/";

  public BelohorizonteSupernossoCrawler(Session session) {
    super(session);
    super.config.setFetcher(FetchMode.FETCHER);
  }

  @Override
  public void handleCookiesBeforeFetch() {
    Logging.printLogDebug(logger, session, "Adding cookie...");

    this.cookies = CrawlerUtils.fetchCookiesFromAPage(HOME_PAGE, Arrays.asList("JSESSIONID"), "www.supernossoemcasa.com.br", "/e-commerce/", cookies,
        session, new HashMap<>(), dataFetcher);
  }

  @Override
  protected JSONObject fetch() {
    JSONObject api = new JSONObject();

    String url = session.getOriginalURL();

    if (url.contains("/p/")) {
      String id = url.split("/p/")[1].split("/")[0];
      String apiUrl = "https://www.supernossoemcasa.com.br/e-commerce/api/products/" + id;

      Map<String, String> headers = new HashMap<>();
      headers.put("Accept", "application/json, text/javascript, */*; q=0.01");

      // request with fetcher
      Request request = RequestBuilder.create().setUrl(apiUrl).setCookies(cookies).setHeaders(headers).build();
      String page = this.dataFetcher.get(session, request).getBody();

      if (page == null || page.isEmpty()) {
        page = new ApacheDataFetcher().get(session, request).getBody();
      }

      api = CrawlerUtils.stringToJson(page);
    }

    return api;
  }

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public List<Product> extractInformation(JSONObject json) throws Exception {
    super.extractInformation(json);
    List<Product> products = new ArrayList<>();

    if (json.has("sku")) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = json.get("sku").toString();
      String internalPid = crawlInternalPid(json);
      String name = crawlName(json);
      Float price = crawlPrice(json);
      Prices prices = crawlPrices(price, json);
      Integer stock = crawlStock(json);
      boolean available = stock != null && stock > 0;
      CategoryCollection categories = crawlCategories(json);
      String primaryImage = crawlPrimaryImage(json);
      String secondaryImages = crawlSecondaryImages(json);
      String description = crawlDescription(json);
      Marketplace marketplace = crawlMarketplace();

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).setMarketplace(marketplace).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private String crawlInternalPid(JSONObject json) {
    String internalPid = null;

    if (json.has("id")) {
      internalPid = json.getString("id");
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

    if (json.has("stockQuantity")) {
      Object stc = json.get("stockQuantity");

      if (stc instanceof Integer) {
        stock = (Integer) stc;
      }
    }

    return stock;
  }

  private Float crawlPrice(JSONObject json) {
    Float price = null;

    if (json.has("price")) {
      Object priceO = json.get("price");

      if (priceO instanceof Double) {
        Double priceD = json.getDouble("price");
        price = MathUtils.normalizeTwoDecimalPlaces(priceD.floatValue());
      }
    }

    return price;
  }

  private Marketplace crawlMarketplace() {
    return new Marketplace();
  }


  private String crawlPrimaryImage(JSONObject json) {
    String primaryImage = null;

    if (json.has("mainImageUrl")) {
      Object img = json.get("mainImageUrl");

      if (img instanceof String) {
        primaryImage = img.toString();
      }
    }

    return primaryImage;
  }

  /**
   * @param doc
   * @return
   */
  private String crawlSecondaryImages(JSONObject json) {
    String secondaryImages = null;

    if (json.has("additionalImagesUrl")) {
      JSONArray images = json.getJSONArray("additionalImagesUrl");

      if (images.length() > 0) {
        secondaryImages = images.toString();
      }
    }

    return secondaryImages;
  }

  /**
   * @param document
   * @return
   */
  private CategoryCollection crawlCategories(JSONObject json) {
    CategoryCollection categories = new CategoryCollection();

    String mainCategoryId = json.has("mainCategoryId") && !json.isNull("mainCategoryId") ? json.getString("mainCategoryId") : null;
    String mainCategoryName = json.has("mainCategoryName") && !json.isNull("mainCategoryName") ? json.getString("mainCategoryName") : null;

    if (mainCategoryName != null) {
      categories.add(mainCategoryName);
    }

    if (mainCategoryId != null && json.has("categories")) {
      JSONArray categoriesIds = json.getJSONArray("categories");

      for (int i = 0; i < categoriesIds.length(); i++) {
        String categoryId = categoriesIds.getString(i);

        if (!mainCategoryId.equals(categoryId)) {
          String categoryRequestURL = "https://www.supernossoemcasa.com.br/e-commerce/api/category/" + categoryId;

          Request request = RequestBuilder.create().setUrl(categoryRequestURL).setCookies(cookies).build();
          JSONObject categoryRequestResponse = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

          if (categoryRequestResponse.has("parentId")) {
            Object parentId = categoryRequestResponse.get("parentId");

            if (parentId instanceof String) {
              String parentCategoryRequestURL = "https://www.supernossoemcasa.com.br/e-commerce/api/category/" + parentId.toString();

              Request requestParent = RequestBuilder.create().setUrl(parentCategoryRequestURL).setCookies(cookies).build();
              JSONObject parentCategoryRequestResponse = CrawlerUtils.stringToJson(this.dataFetcher.get(session, requestParent).getBody());

              if (parentCategoryRequestResponse.has("name")) {
                Object cat2 = parentCategoryRequestResponse.get("name");

                if (cat2 instanceof String) {
                  categories.add(cat2.toString());
                }
              }
            }
          }
        }
      }
    }

    categories.add(mainCategoryName);

    return categories;
  }

  private String crawlDescription(JSONObject json) {
    StringBuilder description = new StringBuilder();

    if (json.has("description")) {
      Object desc = json.get("description");

      if (desc instanceof String) {
        description.append(desc.toString());
      }
    }

    if (json.has("additionalAttributes")) {
      Object obj = json.get("additionalAttributes");

      if (obj instanceof JSONObject) {
        JSONObject additionalAttributes = ((JSONObject) obj);
        Set<?> attributes = additionalAttributes.keySet();

        description.append("<div id=\"itensAdicionais\"> <table>");

        for (Object key : attributes) {
          String label = key.toString();
          String value = additionalAttributes.get(label).toString();

          if (!value.equalsIgnoreCase("null")) {
            value = value.replace("[", "").replace("]", "").replace("\"", "");

            description.append("<tr> <td> " + label + " <td> <td> " + value + " </td> </tr>");
          }
        }

        description.append("</table></div>");
      }
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
      // prices.setBankTicketPrice(price);

      if (jsonSku.has("oldPrice")) {
        String text = jsonSku.get("oldPrice").toString().replaceAll("[^0-9.]", "");

        if (!text.isEmpty()) {
          prices.setPriceFrom(Double.parseDouble(text));
        }
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.CABAL.toString(), installmentPriceMap);
    }

    return prices;
  }

  protected String getRequestBody(InputStream t) throws IOException {
    InputStreamReader isr = new InputStreamReader(t, "utf-8");
    BufferedReader br = new BufferedReader(isr);

    int b;
    StringBuilder buf = new StringBuilder(512);
    while ((b = br.read()) != -1) {
      buf.append((char) b);
    }

    br.close();
    isr.close();

    return buf.toString();
  }
}
