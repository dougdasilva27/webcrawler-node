package br.com.lett.crawlernode.crawlers.corecontent.peru;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public class PeruInkafarmaCrawler extends Crawler {

  private static final String GOOGLE_KEY = "AIzaSyC2fWm7Vfph5CCXorWQnFqepO8emsycHPc";

  public PeruInkafarmaCrawler(Session session) {
    super(session);
    super.config.setFetcher(FetchMode.FETCHER);
  }

  @Override
  protected Object fetch() {
    JSONObject skuJson = new JSONObject();

    String originalUrl = session.getOriginalURL();
    if (originalUrl.contains("?") && originalUrl.contains("sku=")) {
      Map<String, String> headersToken = new HashMap<>();
      headersToken.put(HttpHeaders.CONTENT_TYPE, "application/json");

      Request requestToken = RequestBuilder.create()
          .setUrl("https://www.googleapis.com/identitytoolkit/v3/relyingparty/signupNewUser?key=" + GOOGLE_KEY)
          .setPayload("{\"returnSecureToken\":true}")
          .setHeaders(headersToken)
          .build();

      Response response = this.dataFetcher.post(session, requestToken);
      JSONObject apiTokenJson = JSONUtils.stringToJson(response.getBody());
      if (apiTokenJson.has("idToken") && !apiTokenJson.isNull("idToken")) {
        String accesToken = apiTokenJson.get("idToken").toString();
        String parameterSku = null;

        for (String parameter : CommonMethods.getLast(originalUrl.split("\\?")).split("&")) {
          if (parameter.startsWith("sku=")) {
            parameterSku = CommonMethods.getLast(parameter.split("="));
          }
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("x-access-token", accesToken);
        headers.put("AndroidVersion", "100000");
        headers.put(HttpHeaders.REFERER, session.getOriginalURL());
        headers.put(HttpHeaders.USER_AGENT,
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36");

        Request request = RequestBuilder.create()
            .setUrl("https://qurswintke.execute-api.us-west-2.amazonaws.com/PROD/product/" + parameterSku)
            .setHeaders(headers)
            .mustSendContentEncoding(false)
            .build();

        try {
          System.err
              .println(Jsoup.connect("https://qurswintke.execute-api.us-west-2.amazonaws.com/PROD/product/" + parameterSku).headers(headers).get());
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        skuJson = JSONUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
      }
    }

    return skuJson;
  }

  public List<Product> extractInformation(JSONObject jsonSku) throws Exception {
    List<Product> products = new ArrayList<>();

    String productUrl = session.getOriginalURL();

    if (jsonSku.has("id") && !jsonSku.isNull("id")) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = jsonSku.get("id").toString();
      String internalPid = internalId;
      String name = JSONUtils.getStringValue(jsonSku, "name");
      Integer stock = JSONUtils.getIntegerValueFromJSON(jsonSku, "stock", 0);
      boolean available = stock > 0;
      Float price = available ? JSONUtils.getFloatValueFromJSON(jsonSku, "price", true) : null;
      Prices prices = crawlPrices(price);
      String description = crawlDescription(jsonSku);
      String primaryImage = crawlPrimaryImage(jsonSku);
      String secondaryImages = null; // at the time this crawlers was made, there are no secondary images
      CategoryCollection categories = new CategoryCollection();

      // Creating the product
      Product product = ProductBuilder.create()
          .setUrl(productUrl)
          .setInternalId(internalId)
          .setInternalPid(internalPid)
          .setName(name)
          .setPrice(price)
          .setPrices(prices)
          .setAvailable(available)
          .setCategory1(categories.getCategory(0))
          .setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2))
          .setPrimaryImage(primaryImage)
          .setSecondaryImages(secondaryImages)
          .setDescription(description)
          .setStock(stock)
          .setMarketplace(new Marketplace())
          .build();

      products.add(product);
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private String crawlPrimaryImage(JSONObject json) {
    String primaryImage = null;

    JSONArray images = JSONUtils.getJSONArrayValue(json, "imageList");
    for (Object o : images) {
      if (o instanceof JSONObject) {
        JSONObject imageJson = (JSONObject) o;

        String image = JSONUtils.getStringValue(imageJson, "url");
        if (image != null) {
          if (primaryImage != null && primaryImage.endsWith("X.png")) {
            break;
          } else if (primaryImage == null || image.endsWith("X.png")) {
            primaryImage = image;
          } else if (image.endsWith("L.png")) {
            primaryImage = image;
          } else if (primaryImage != null && !primaryImage.endsWith("L.png")) {
            primaryImage = image;
          }
        }
      }
    }

    return primaryImage;
  }

  private String crawlDescription(JSONObject json) {
    StringBuilder description = new StringBuilder();

    Map<String, String> descriptionsKeys = new HashMap<>();
    descriptionsKeys.put("shortDescription", "");
    descriptionsKeys.put("longDescription", "Descripción");
    descriptionsKeys.put("howToConsume", "Administración");
    descriptionsKeys.put("precautions", "Precauciones");
    descriptionsKeys.put("sideEffects", "Contraindicaciones");

    for (Entry<String, String> entry : descriptionsKeys.entrySet()) {
      if (json.has(entry.getKey()) && !json.isNull(entry.getKey())) {
        description.append("<div id=\"" + entry.getKey() + "\">");
        description.append("<h5> " + entry.getValue() + "</h5>");
        description.append("<p> " + json.get(entry.getKey()) + "</p>");
        description.append("</div>");
      }
    }

    return description.toString();
  }

  /**
   * In this site has no information of installments
   * 
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price) {
    Prices p = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();
      installmentPriceMap.put(1, price);

      p.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      p.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      p.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
    }

    return p;
  }
}
