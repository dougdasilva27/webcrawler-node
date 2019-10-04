package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.test.Test;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import models.prices.Prices;

/**
 * Date: 09/07/2019
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilVilanovaCrawler extends Crawler {

  public static final String HOME_PAGE = "https://www.vilanova.com.br/";
  private static final String IMAGES_HOST = "i2-vilanova.a8e.net.br";

  private static final String LOGIN_URL = "https://www.vilanova.com.br/Cliente/Logar";
  private static final String CNPJ = "33.033.028%2F0040-90";
  private static final String PASSWORD = "Mudar123";

  private LettProxy proxy = null;
  private static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36";

  public BrasilVilanovaCrawler(Session session) {
    super(session);
    super.config.setFetcher(FetchMode.FETCHER);
  }

  @Override
  public void handleCookiesBeforeFetch() {
    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.USER_AGENT, USER_AGENT);

    Request requestHome = RequestBuilder.create()
        .setUrl(HOME_PAGE)
        .setHeaders(headers)
        .setProxyservice(
            Arrays.asList(
                ProxyCollection.BONANZA,
                ProxyCollection.INFATICA_RESIDENTIAL_BR
            ))
        .build();
    Response response = this.dataFetcher.get(session, requestHome);
    this.cookies = CrawlerUtils.fetchCookiesFromAPage(HOME_PAGE, null, "www.vilanova.com.br", "/", cookies, session, new HashMap<>(), dataFetcher);
    this.proxy = response.getProxyUsed();

    StringBuilder payload = new StringBuilder();
    payload.append("usuario_cnpj=").append(CNPJ);
    payload.append("&usuario_senha=").append(PASSWORD);

    headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=UTF-8");
    headers.put("sec-fetch-mode", "cors");
    headers.put("sec-fetch-site", "same-origin");
    headers.put("origin", "https://www.vilanova.com.br");
    headers.put("X-Requested-With", "XMLHttpRequest");

    Request request = RequestBuilder.create()
        .setUrl(LOGIN_URL)
        .setPayload(payload.toString())
        .setCookies(this.cookies)
        .setHeaders(headers)
        .setProxy(this.proxy)
        .build();

    Response loginResponse = this.dataFetcher.post(session, request);
    List<Cookie> cookiesResponse = loginResponse.getCookies();

    for (Cookie cookieResponse : cookiesResponse) {
      BasicClientCookie cookie = new BasicClientCookie(cookieResponse.getName(), cookieResponse.getValue());
      cookie.setDomain("www.vilanova.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
    }

    Request requestHome2 = RequestBuilder.create()
        .setUrl(HOME_PAGE)
        .setHeaders(headers)
        .setCookies(this.cookies)
        .setProxy(this.proxy)
        .build();

    CommonMethods.saveDataToAFile(Jsoup.parse(this.dataFetcher.get(session, requestHome2).getBody()), Test.pathWrite + "ILUSAO.html");

    System.err.println(cookies);
  }

  @Override
  protected Object fetch() {
    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.USER_AGENT, USER_AGENT);

    Request request = RequestBuilder.create()
        .setUrl(session.getOriginalURL())
        .setCookies(this.cookies)
        .setProxy(this.proxy)
        .build();

    return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    CommonMethods.saveDataToAFile(doc, Test.pathWrite + "VILANOVA.html");

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
