
package br.com.lett.crawlernode.crawlers.corecontent.campogrande;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.LettProxy;
import br.com.lett.crawlernode.core.fetcher.methods.GETFetcher;
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

  @Override
  public void handleCookiesBeforeFetch() {
    Logging.printLogDebug(logger, session, "Adding cookie...");

    this.userAgent = DataFetcher.randUserAgent();

    Map<String, String> cookiesMap = DataFetcher.fetchCookies(session, HOME_PAGE, cookies, this.userAgent, 1);

    for (Entry<String, String> entry : cookiesMap.entrySet()) {
      BasicClientCookie cookie = new BasicClientCookie(entry.getKey(), entry.getValue());
      cookie.setDomain("www.comperdelivery.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
    }

    Map<String, String> cookiesMap2 =
        DataFetcher.fetchCookies(session, "https://www.comperdelivery.com.br/store/SetStore?storeId=6602", cookies, this.userAgent, 1);
    for (Entry<String, String> entry : cookiesMap2.entrySet()) {
      BasicClientCookie cookie = new BasicClientCookie(entry.getKey(), entry.getValue());
      cookie.setDomain("www.comperdelivery.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
    }
  }

  @Override
  protected Object fetch() {
    LettProxy proxy = session.getRequestProxy("https://www.comperdelivery.com.br/store/SetStore?storeId=6602");
    String page = GETFetcher.fetchPageGET(session, session.getOriginalURL(), cookies, this.userAgent, proxy, 1);

    return Jsoup.parse(page);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      JSONObject productJson = CrawlerUtils.selectJsonFromHtml(doc, "script", "dataLayer.push(", ");", false);

      String internalId = crawlInternalId(productJson);
      String internalPid = crawlInternalPid(productJson);
      String name = crawlName(productJson);
      boolean available = crawlAvailability(productJson);
      Float price = available ? crawlPrice(productJson) : null;
      Prices prices = available ? crawlPrices(price) : new Prices();
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc);
      String description = crawlDescription(doc);
      Integer stock = null;
      Marketplace marketplace = crawlMarketplace();

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).setMarketplace(marketplace).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
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
      if (product.has("RKProductOffer")) {
        price = Float.parseFloat(product.get("RKProductOffer").toString());

        if (price < 0.1) {
          price = null;
        }
      }
    } catch (NumberFormatException e) {
      Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
    }

    try {
      if (price == null && product.has("RKProductPrice")) {
        price = Float.parseFloat(product.get("RKProductPrice").toString());
      }
    } catch (NumberFormatException e) {
      Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
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
   * at the time this crawler was made,there was no secondary images
   * 
   * @param doc
   * @return
   */
  private String crawlSecondaryImages(Document doc) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
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
  private Prices crawlPrices(Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();

      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
    }

    return prices;
  }

}
