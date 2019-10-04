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
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
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

public class BrasilLojamondelezCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.lojamondelez.com.br/";

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

    if (!doc.select(".infoProduct").isEmpty()) {
      String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-ean .value", true);
      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".infoProduct [data-product-id]", "data-product-id");
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.nameProduct", true);
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".bestPrice .val", null, true, ',', session);
      boolean available = doc.selectFirst(".withStock") != null;
      Prices prices = scrapPrices(price);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "a.thumb-link", Arrays.asList("data-zoom-image", "data-img-medium", "href"),
          "https", "i1-mondelez.a8e.net.br");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "a.thumb-link", Arrays.asList("data-zoom-image", "data-img-medium",
          "href"), "https", "i1-mondelez.a8e.net.br", primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".description-product"));
      List<String> eans = Arrays.asList(internalId);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".product-breadcrumb a[href]:not(:first-child)");

      // Creating the product
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
          .setMarketplace(new Marketplace())
          .setEans(eans)
          .build();

      products.add(product);
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private Prices scrapPrices(Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installments = new HashMap<>();
      installments.put(1, price);

      prices.setBankTicketPrice(price);
      prices.insertCardInstallment(Card.VISA.toString(), installments);
    }

    return prices;
  }
}
