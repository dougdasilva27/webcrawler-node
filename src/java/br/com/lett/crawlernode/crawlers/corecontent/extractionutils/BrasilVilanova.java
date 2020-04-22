package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.methods.JavanetDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import models.prices.Prices;
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public abstract class BrasilVilanova extends Crawler {

  public static final String HOME_PAGE = "https://www.vilanova.com.br/";
  private static final String IMAGES_HOST = "i2-vilanova.a8e.net.br";

  private static final String LOGIN_URL = "https://www.vilanova.com.br/Cliente/Logar";
  private final String CNPJ = getCnpj();
  private final String PASSWORD = getPassword();

  public BrasilVilanova(Session session) {
    super(session);
    super.config.setFetcher(FetchMode.APACHE);
  }

  public abstract String getCnpj();

  public abstract String getPassword();

  private String cookiePHPSESSID = null;

  @Override
  public void handleCookiesBeforeFetch() {

    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=UTF-8");
    headers.put("sec-fetch-mode", "cors");
    headers.put("origin", "https://www.vilanova.com.br");
    headers.put("sec-fetch-site", "same-origin");
    headers.put("x-requested-with", "XMLHttpRequest");

    String payload = "usuario_cnpj=" + CNPJ + "&usuario_senha=" + PASSWORD;
    Request request = RequestBuilder.create().setUrl(LOGIN_URL)
        .setPayload(payload)
        .setHeaders(headers).build();
    Response response = this.dataFetcher.post(session, request);

    cookies = response.getCookies();
    cookies.removeIf(cookie -> cookie.getValue().equals("deleted"));
    for (Cookie cookieResponse : cookies) {
      if (cookieResponse.getName().equalsIgnoreCase("PHPSESSID")) {
        cookiePHPSESSID = cookieResponse.getValue();
      }
    }
  }

  @Override
  protected Object fetch() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Cookie", "PHPSESSID=" + cookiePHPSESSID);

    Request request = RequestBuilder.create()
        .setUrl(session.getOriginalURL())
        .setHeaders(headers)
        .build();

    return Jsoup.parse(new JavanetDataFetcher().get(session, request).getBody());
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session,
          "Product page identified: " + this.session.getOriginalURL());

      JSONArray productJsonArray = CrawlerUtils
          .selectJsonArrayFromHtml(doc, "script", "var dataLayer = ", ";", false, true);
      JSONObject productJson = extractProductData(productJsonArray);

      String internalPid = crawlInternalPid(productJson);
      List<String> eans = Collections
          .singletonList(CrawlerUtils.scrapStringSimpleInfo(doc, ".product-ean .value", true));
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "#Breadcrumbs li a", true);
      String description = CrawlerUtils
          .scrapElementsDescription(doc, Collections.singletonList("#info-abas-mobile"));
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#imagem-produto #elevateImg",
          Arrays.asList("data-zoom-image", "href", "src"),
          "https", IMAGES_HOST);
      String secondaryImages = CrawlerUtils
          .scrapSimpleSecondaryImages(doc, "#imagem-produto #elevateImg",
              Arrays.asList("data-zoom-image", "href",
                  "src"),
              "https", IMAGES_HOST, primaryImage);

      JSONObject productsJson = getSkusList(CrawlerUtils
          .scrapStringSimpleInfoByAttribute(doc, ".variacao-container", "data-produtoean"));

      for (String key : productsJson.keySet()) {
        JSONObject skuJson = productsJson.optJSONObject(key);

        String internalId = skuJson.optString("Id");
        String name = skuJson.optString("Nome") + " " + skuJson.optString("Picking") + "un";
        int stock = JSONUtils.getJSONValue(skuJson, "Estoque").optInt("Disponivel");

        float price = skuJson.optFloat("PrecoPor", 0F);
        Prices prices = scrapPrices(price, skuJson);

        if (price != 0F) {
          price = skuJson.optFloat("PrecoPorSemPromocao", 0F);
          prices.setPriceFrom(null);
        }

        boolean available = price != 0F;

        Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
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
            .setEans(eans)
            .setStock(stock)
            .build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private JSONObject extractProductData(JSONArray productJsonArray) {
    JSONObject firstObjectFromArray =
        productJsonArray.length() > 0 ? productJsonArray.getJSONObject(0) : new JSONObject();
    return firstObjectFromArray.has("productData") ? firstObjectFromArray
        .getJSONObject("productData") : firstObjectFromArray;
  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".container #detalhes-container").isEmpty();
  }

  private String crawlInternalPid(JSONObject productJson) {
    String internalPid = null;

    if (productJson.has("productID") && !productJson.isNull("productID")) {
      internalPid = productJson.get("productID").toString();
    }

    return internalPid;
  }

  private JSONObject getSkusList(String ean) {
    Map<String, String> headers = new HashMap<>();
    StringBuilder cookiebuilder = new StringBuilder();

    for (Cookie cookie : cookies) {
      cookiebuilder.append(cookie.getName()).append("=").append(cookie.getValue()).append("; ");
    }
    headers.put("cookie", cookiebuilder.toString());
    Response response = dataFetcher.get(session,
        RequestBuilder.create().setUrl("https://www.vilanova.com.br/Produto/Variacoes/" + ean)
            .setHeaders(headers)
            .build());

    return JSONUtils.stringToJson(response.getBody());
  }

  private Prices scrapPrices(Float price, JSONObject json) {
    Prices prices = new Prices();

    if (price != null && price > 0) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);

      prices.setPriceFrom(json.optDouble("PrecoPorSemPromocao"));
      prices.setBankTicketPrice(price);
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
    }

    return prices;
  }
}
