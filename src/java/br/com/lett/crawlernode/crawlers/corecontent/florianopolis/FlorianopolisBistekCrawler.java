package br.com.lett.crawlernode.crawlers.corecontent.florianopolis;


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
import br.com.lett.crawlernode.core.fetcher.FetchMode;
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
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

public class FlorianopolisBistekCrawler extends Crawler {

  private static final String HOME_PAGE = "http://www.bistekonline.com.br/";
  private static final String HOST = "www.bistekonline.com.br";
  private static final String CEP = "88066-000";

  public FlorianopolisBistekCrawler(Session session) {
    super(session);
    super.config.setFetcher(FetchMode.FETCHER);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
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
      cookie.setDomain(HOST);
      cookie.setPath("/");
      this.cookies.add(cookie);
    }

    Request request2 = RequestBuilder.create().setUrl("https://www.bistekonline.com.br/store/SetStoreByZipCode?zipCode=" + CEP).setProxy(proxyUsed)
        .setCookies(cookies).setHeaders(headers).build();
    this.dataFetcher.get(session, request2);

    BasicClientCookie cookieM = new BasicClientCookie("MultiStoreId", "04010000000000000000000010100000");
    cookieM.setDomain(HOST);
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
      Logging.printLogDebug(logger, session, "Product page identified: " + session.getOriginalURL());

      String internalId = crawlInternalId(doc);
      String internalPid = crawlInternalPid(doc);
      String name = crawlName(doc);
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".main-content #lblPrecoPor strong", null, true, ',', session);
      boolean available = price != null;
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".collum.images #hplAmpliar:not([href=\"#\"]), #big_photo_container img",
          Arrays.asList("href", "src"), "https", HOST);
      String secondaryImages = null;
      String description = crawlDescription(doc);
      Prices prices = crawlPrices(doc, price);
      String ean = scrapEan(doc);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setMarketplace(new Marketplace()).setEans(Arrays.asList(ean)).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page: " + session.getOriginalURL());
    }

    return products;
  }

  private static boolean isProductPage(Document doc) {
    return doc.select("#info-product").first() != null;
  }

  private static String crawlInternalId(Document document) {
    String internalId = null;
    Element internalIdElement = document.selectFirst("#liCodigoInterno #productInternalCode");

    if (internalIdElement != null) {
      internalId = internalIdElement.text().trim();
    }

    return internalId;
  }

  private static String crawlInternalPid(Document document) {
    String internalPid = null;
    Element pid = document.select("#ProdutoCodigo").first();

    if (pid != null) {
      internalPid = pid.val();
    }

    return internalPid;
  }

  private static String crawlName(Document document) {
    String name = null;
    Element nameElement = document.selectFirst(".main-content h1.name.fn");

    if (nameElement != null) {
      name = nameElement.text().trim();
    }

    return name;
  }

  /**
   * @param document
   * @return
   */
  private static CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select("#breadcrumbs span a[href] span");

    for (int i = 1; i < elementCategories.size(); i++) {
      String cat = elementCategories.get(i).ownText().trim();

      if (!cat.isEmpty()) {
        categories.add(cat);
      }
    }

    return categories;
  }

  private static String crawlDescription(Document document) {
    String description = "";
    Element descriptionElement = document.select("#panCaracteristica").first();

    if (descriptionElement != null) {
      description = description + descriptionElement.html();
    }

    return description;
  }

  private String scrapEan(Document doc) {
    String ean = null;

    JSONObject dataLayer = CrawlerUtils.selectJsonFromHtml(doc, "script", "dataLayer.push(", ");", true, true);
    if (dataLayer.has("RKProductEan13")) {
      ean = dataLayer.get("RKProductEan13").toString();
    }

    return ean;
  }

  /**
   * In this market has no bank slip payment method
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();
      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);

      prices.setPriceFrom(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".main-content #lblPreco.price-from", null, true, ',', session));

      Element installments = doc.select(".main-content #lblParcelamento").first();

      if (installments != null) {
        Element installmentElement = installments.select("#lblParcelamento1 > strong").first();

        if (installmentElement != null) {
          Integer installment = Integer.parseInt(installmentElement.text().replaceAll("[^0-9]", ""));

          Element valueElement = installments.select("#lblParcelamento2 > strong").first();

          if (valueElement != null) {
            Float value = MathUtils.parseFloatWithComma(valueElement.text());

            installmentPriceMap.put(installment, value);
          }
        }
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
    }

    return prices;
  }
}
