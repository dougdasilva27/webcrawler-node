
package br.com.lett.crawlernode.crawlers.corecontent.campogrande;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
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
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

public class CampograndeComperCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.comperdelivery.com.br/";

  public CampograndeComperCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  private String userAgent;
  private LettProxy proxyUsed;

  @Override
  public void handleCookiesBeforeFetch() {
    this.userAgent = FetchUtilities.randUserAgent();

    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.USER_AGENT, this.userAgent);

    Request request = RequestBuilder.create().setUrl(HOME_PAGE).setCookies(cookies).setHeaders(headers)
        .setProxyservice(Arrays.asList(ProxyCollection.BUY, ProxyCollection.BONANZA, ProxyCollection.NO_PROXY)).build();
    Response response = this.dataFetcher.get(session, request);

    this.proxyUsed = response.getProxyUsed();

    for (Cookie cookieResponse : response.getCookies()) {
      BasicClientCookie cookie = new BasicClientCookie(cookieResponse.getName(), cookieResponse.getValue());
      cookie.setDomain("www.comperdelivery.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
    }

    Request request2 = RequestBuilder.create().setUrl("https://www.comperdelivery.com.br/store/SetStore?storeId=6602").setProxy(proxyUsed)
        .setCookies(cookies).setHeaders(headers).setFollowRedirects(false).build();
    this.dataFetcher.get(session, request2);

    BasicClientCookie cookieM = new BasicClientCookie("MultiStoreId", "02010714110000101010000010001000");
    cookieM.setDomain("www.comperdelivery.com.br");
    cookieM.setPath("/");
    this.cookies.add(cookieM);
  }

  @Override
  protected Object fetch() {
    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.USER_AGENT, this.userAgent);

    Request request = RequestBuilder.create().setUrl(session.getOriginalURL()).setCookies(cookies).setHeaders(headers).setProxy(proxyUsed).build();
    return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      JSONObject productJson = CrawlerUtils.selectJsonFromHtml(doc, "script", "dataLayer.push(", ");", false, false);

      String internalId = crawlInternalId(productJson);
      String internalPid = crawlInternalPid(productJson);
      String name = crawlName(productJson);
      boolean available = crawlAvailability(productJson);
      Float price = available ? crawlPrice(productJson) : null;
      Prices prices = available ? crawlPrices(price, doc) : new Prices();
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".images .thumbs li[style~=block] img", Arrays.asList("src"), "https",
          "www.comperdelivery.com.br", primaryImage);
      String description = crawlDescription(doc);
      Integer stock = null;
      Marketplace marketplace = crawlMarketplace();
      String ean = productJson.has("RKProductEan13") ? productJson.getString("RKProductEan13") : null;

      List<String> eans = new ArrayList<>();
      eans.add(ean);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).setMarketplace(marketplace).setEans(eans).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return doc.select("#info-product").first() != null;
  }

  private String crawlInternalId(JSONObject product) {
    String internalId = null;

    if (product.has("RKProductWebID")) {
      internalId = product.getString("RKProductWebID");
    }

    return internalId;
  }

  private String crawlInternalPid(JSONObject product) {
    String internalPid = null;

    if (product.has("RKProductID")) {
      internalPid = product.getString("RKProductID");
    }

    return internalPid;
  }

  private String crawlName(JSONObject product) {
    String name = null;

    if (product.has("RKProductName")) {
      name = product.getString("RKProductName");
    }

    return name;
  }

  private Float crawlPrice(JSONObject product) {
    Float price = null;

    try {
      if (product.has("RKProductPrice")) {
        price = Float.parseFloat(product.get("RKProductPrice").toString());
      }
    } catch (NumberFormatException e) {
      Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
    }

    return price;
  }

  private Marketplace crawlMarketplace() {
    return new Marketplace();
  }


  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;
    Element elementPrimaryImage = doc.select("#Zoom1").first();

    if (elementPrimaryImage != null) {
      primaryImage = elementPrimaryImage.attr("href");

      if (primaryImage.contains(".gif")) {
        Element img = elementPrimaryImage.select("> img").first();

        if (img != null) {
          primaryImage = img.attr("src");
        }
      }
    }

    if (primaryImage != null && !primaryImage.contains(".comper")) {
      primaryImage = (HOME_PAGE + primaryImage).replace("br//", "br/");
    }

    return primaryImage;
  }

  /**
   * @param document
   * @return
   */
  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".breadcrumbs li a");

    for (int i = 0; i < elementCategories.size(); i++) {
      String cat = elementCategories.get(i).ownText().trim();

      if (!cat.isEmpty()) {
        categories.add(cat);
      }
    }

    return categories;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Element elementDescription = doc.select("#ajaxDescription").first();

    if (elementDescription != null) {
      description.append(elementDescription.html());
    }

    Element elementSpec = doc.select("#ajaxSpecification").first();

    if (elementSpec != null) {
      description.append(elementSpec.html());
    }

    return description.toString();
  }

  private boolean crawlAvailability(JSONObject product) {
    return product.has("RKProductAvailable") && product.get("RKProductAvailable").toString().equals("1");
  }

  /**
   * In this market, installments not appear in product page
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();

      installmentPriceMap.put(1, price);
      // prices.setBankTicketPrice(price);

      Element priceFrom = doc.select("#lblPreco.price-from").first();
      if (priceFrom != null) {
        prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceFrom.text()));
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
    }

    return prices;
  }

}
