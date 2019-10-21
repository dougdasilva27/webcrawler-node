package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import models.prices.Prices;

public class BrasilLojamondelezCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.lojamondelez.com.br/";
  private static final String IMAGES_HOST = "images-mondelez.ifcshop.com.br";

  private static final String LOGIN_URL = "https://secure.lojamondelez.com.br/ckout/api/v2/customer/login";
  private static final String CNPJ = "33033028004090";
  private static final String PASSWORD = "monica08";

  public BrasilLojamondelezCrawler(Session session) {
    super(session);
    super.config.setFetcher(FetchMode.FETCHER);
  }

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }


  @Override
  public void handleCookiesBeforeFetch() {
    JSONObject payload = new JSONObject();
    payload.put("login", CNPJ);
    payload.put("password", PASSWORD);

    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=UTF-8");
    headers.put("sec-fetch-mode", "cors");
    headers.put("origin", "https://secure.lojamondelez.com.br");
    headers.put("sec-fetch-site", "same-origin");
    headers.put("x-requested-with", "XMLHttpRequest");
    headers.put("cookie", "ShopCartMONDELEZ=");

    String payloadString = "jsonData=";

    try {
      payloadString += URLEncoder.encode(payload.toString(), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
    }


    Request request = RequestBuilder.create().setUrl(LOGIN_URL).setPayload(payloadString).setHeaders(headers).build();
    List<Cookie> cookiesResponse = this.dataFetcher.post(session, request).getCookies();

    for (Cookie cookieResponse : cookiesResponse) {
      BasicClientCookie cookie = new BasicClientCookie(cookieResponse.getName(), cookieResponse.getValue());
      cookie.setDomain(".lojamondelez.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
    }
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      JSONArray productJsonArray = CrawlerUtils.selectJsonArrayFromHtml(doc, "script", "var dataLayer = ", ";", false, true);
      JSONObject productJson = extractProductData(productJsonArray);

      String internalPid = crawlInternalPid(productJson);
      List<String> eans = Arrays.asList(CrawlerUtils.scrapStringSimpleInfo(doc, ".product-ean .value", true));
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "#Breadcrumbs li a", true);
      String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList("#info-abas-mobile"));
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#imagem-produto #elevateImg", Arrays.asList("data-zoom-image", "href", "src"),
          "https", IMAGES_HOST);
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#imagem-produto #elevateImg", Arrays.asList("data-zoom-image", "href",
          "src"),
          "https", IMAGES_HOST, primaryImage);

      JSONArray productsArray =
          productJson.has("productSKUList") && !productJson.isNull("productSKUList") ? productJson.getJSONArray("productSKUList") : new JSONArray();

      for (Object obj : productsArray) {
        JSONObject skuJson = (JSONObject) obj;

        String internalId = crawlInternalId(skuJson);
        String name = crawlName(skuJson);
        Float price = JSONUtils.getFloatValueFromJSON(skuJson, "price", true);

        Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrice(price)
            .setPrices(new Prices())
            .setAvailable(false)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setEans(eans)
            .build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private JSONObject extractProductData(JSONArray productJsonArray) {
    JSONObject firstObjectFromArray = productJsonArray.length() > 0 ? productJsonArray.getJSONObject(0) : new JSONObject();
    return firstObjectFromArray.has("productData") ? firstObjectFromArray.getJSONObject("productData") : firstObjectFromArray;
  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".container #detalhes-container").isEmpty();
  }

  private String crawlInternalId(JSONObject skuJson) {
    String internalId = null;

    if (skuJson.has("sku") && !skuJson.isNull("sku")) {
      internalId = skuJson.get("sku").toString();
    }

    return internalId;
  }

  private String crawlInternalPid(JSONObject productJson) {
    String internalPid = null;

    if (productJson.has("productID") && !productJson.isNull("productID")) {
      internalPid = productJson.get("productID").toString();
    }

    return internalPid;
  }

  private String crawlName(JSONObject skuJson) {
    String name = null;

    if (skuJson.has("name") && skuJson.get("name") instanceof String) {
      name = skuJson.getString("name");
    }

    return name;
  }
}
