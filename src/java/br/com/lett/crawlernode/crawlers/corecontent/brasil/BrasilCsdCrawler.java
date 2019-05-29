package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
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
import models.prices.Prices;

/**
 * 
 * @author gabriel date: 2018-03-15
 */
public class BrasilCsdCrawler extends Crawler {

  public BrasilCsdCrawler(Session session) {
    super(session);
  }

  public static final String HOME_PAGE = "https://www.sitemercado.com.br/supermercadoscidadecancao/londrina-loja-londrina-19-rodocentro-avenida-tiradentes/";
  public static final String LOAD_PAYLOAD = "{\"lojaUrl\":\"londrina-loja-londrina-19-rodocentro-avenida-tiradentes\",\"redeUrl\":\"supermercadoscidadecancao\"}";

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
  }

  @Override
  protected JSONObject fetch() {
    return crawlProductInformatioFromApi(session.getOriginalURL());
  }

  @Override
  public List<Product> extractInformation(JSONObject jsonSku) throws Exception {
    super.extractInformation(jsonSku);
    List<Product> products = new ArrayList<>();

    if (jsonSku.has("idLojaProduto")) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(jsonSku);
      String internalPid = crawlInternalPid(jsonSku);
      CategoryCollection categories = crawlCategories(jsonSku);
      String description = crawlDescription(jsonSku);
      Integer stock = jsonSku.has("quantityStock") && jsonSku.get("quantityStock") instanceof Integer ? jsonSku.getInt("quantityStock") : null;
      boolean available = jsonSku.has("isSale") && !jsonSku.isNull("isSale") && jsonSku.getBoolean("isSale");
      Float price = available ? crawlPrice(jsonSku) : null;
      String primaryImage = crawlPrimaryImage(jsonSku);
      String name = crawlName(jsonSku);
      String secondaryImages = crawlSecondaryImages(jsonSku, primaryImage);
      Double priceFrom = crawlPriceFrom(jsonSku);
      Prices prices = crawlPrices(price, priceFrom);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price).setPrices(prices)
          .setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage)
          .setSecondaryImages(secondaryImages).setDescription(description).setStock(stock).setMarketplace(new Marketplace()).build();

      products.add(product);


    } else {
      Logging.printLogDebug(logger, session, "Not a product page:   " + this.session.getOriginalURL());
    }

    return products;
  }


  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("idLojaProduto")) {
      internalId = json.get("idLojaProduto").toString();
    }

    return internalId;
  }

  private String crawlInternalPid(JSONObject json) {
    String internalId = null;

    if (json.has("idProduct")) {
      internalId = json.get("idProduct").toString();
    }

    return internalId;
  }

  private String crawlName(JSONObject json) {
    String name = null;

    if (json.has("excerpt")) {
      name = json.getString("excerpt");
    }

    return name;
  }

  private Float crawlPrice(JSONObject json) {
    Float price = null;

    if (json.has("unit")) {
      String unit = json.get("unit").toString().replace("null", "").trim();

      if (json.has("prices")) {
        JSONArray prices = json.getJSONArray("prices");

        for (Object obj : prices) {
          JSONObject priceJson = (JSONObject) obj;

          if (!unit.isEmpty() && priceJson.has("unit") && !unit.equals(priceJson.get("unit").toString().trim())) {
            continue;
          }

          if (priceJson.has("price")) {
            Object pObj = priceJson.get("price");

            if (pObj instanceof Double) {
              price = MathUtils.normalizeTwoDecimalPlaces(((Double) pObj).floatValue());

              if (price == 0d) {
                price = null;
              }

              break;
            }
          }
        }
      }
    }

    return price;
  }

  private Double crawlPriceFrom(JSONObject json) {
    Double price = null;

    if (json.has("price_old")) {
      Object pObj = json.get("price_old");

      if (pObj instanceof Double) {
        price = MathUtils.normalizeTwoDecimalPlaces((Double) pObj);

        if (price == 0d) {
          price = null;
        }
      }
    }

    return price;
  }

  private String crawlPrimaryImage(JSONObject json) {
    String primaryImage = null;

    if (json.has("images")) {
      JSONArray images = json.getJSONArray("images");

      if (images.length() > 0) {
        JSONObject imageJson = images.getJSONObject(0);

        if (imageJson.has("img")) {
          primaryImage = imageJson.getString("img");

          if (!primaryImage.startsWith("http")) {
            primaryImage = ("https:" + primaryImage).replace("s:://", "s://");
          }
        }
      }
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(JSONObject json, String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    if (json.has("images") && primaryImage != null) {
      JSONArray images = json.getJSONArray("images");

      for (Object obj : images) {
        JSONObject imageJson = (JSONObject) obj;

        if (imageJson.has("img")) {
          String image = imageJson.getString("img");

          if (!image.startsWith("http")) {
            image = ("https:" + image).replace("s:://", "s://");
          }

          if (!primaryImage.equalsIgnoreCase(image)) {
            secondaryImagesArray.put(image);
          }
        }
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }


  private CategoryCollection crawlCategories(JSONObject json) {
    CategoryCollection categories = new CategoryCollection();

    if (json.has("department")) {
      String category = json.get("department").toString().replace("null", "").trim();

      if (!category.isEmpty()) {
        categories.add(category);
      }
    }

    if (json.has("category")) {
      String category = json.get("category").toString().replace("null", "").trim();

      if (!category.isEmpty()) {
        categories.add(category);
      }
    }

    if (json.has("subCategory")) {
      String category = json.get("subCategory").toString().replace("null", "").trim();

      if (!category.isEmpty()) {
        categories.add(category);
      }
    }

    return categories;
  }


  private String crawlDescription(JSONObject json) {
    StringBuilder description = new StringBuilder();

    if (json.has("description")) {
      description.append(json.get("description"));
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
   * Get the json of gpa api, this api has all info of product
   * 
   * @return
   */
  private JSONObject crawlProductInformatioFromApi(String productUrl) {
    String loadUrl = "https://www.sitemercado.com.br/core/api/v1/b2c/page/load";
    String url = "https://www.sitemercado.com.br/core/api/v1/b2c/product/load?url=" + CommonMethods.getLast(productUrl.split("/")).split("\\?")[0];

    Map<String, String> headers = new HashMap<>();
    headers.put("referer", productUrl);
    headers.put("accept", "application/json, text/plain, */*");
    headers.put("content-type", "application/json");
    headers.put("sm-mmc", fetchApiVersion(session, session.getOriginalURL()));

    Request request = RequestBuilder.create().setUrl(loadUrl).setCookies(cookies).setHeaders(headers).setPayload(LOAD_PAYLOAD).build();
    Map<String, String> responseHeaders = new FetcherDataFetcher().post(session, request).getHeaders();

    if (responseHeaders.containsKey("sm-token")) {
      headers.put("sm-token", responseHeaders.get("sm-token"));
    }

    headers.remove("content-type");

    Request requestApi = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).build();
    return CrawlerUtils.stringToJson(this.dataFetcher.get(session, requestApi).getBody());
  }


  public static String fetchApiVersion(Session session, String url) {
    String version = null;

    String loadUrl = "https://www.sitemercado.com.br/core/api/v1/b2c/page/load";
    Map<String, String> headers = new HashMap<>();
    headers.put("referer", url);
    headers.put("accept", "application/json, text/plain, */*");
    headers.put("content-type", "application/json");

    Request request = RequestBuilder.create().setUrl(loadUrl).setHeaders(headers).setPayload(LOAD_PAYLOAD).setIgnoreStatusCode(false).build();
    Map<String, String> responseHeaders = new FetcherDataFetcher().post(session, request).getHeaders();

    if (responseHeaders.containsKey("sm-mmc")) {
      version = responseHeaders.get("sm-mmc");
    }

    return version;
  }
}
