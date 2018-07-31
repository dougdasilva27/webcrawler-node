package br.com.lett.crawlernode.crawlers.corecontent.brasil;


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
import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/**
 * date: 27/03/2018
 * 
 * @author gabriel
 *
 */

public class BrasilPetzCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.petz.com.br/";

  public BrasilPetzCrawler(Session session) {
    super(session);
  }

  private String userAgent;

  @Override
  public void handleCookiesBeforeFetch() {
    this.userAgent = DataFetcher.randUserAgent();
    Map<String, String> cookiesMap;

    if (DataFetcher.mustUseFetcher(1, session)) {
      Map<String, String> headers = new HashMap<>();
      headers.put("User-Agent", this.userAgent);

      JSONObject fetcherPayload =
          POSTFetcher.fetcherPayloadBuilder(HOME_PAGE, DataFetcher.GET_REQUEST, true, null, headers, session.getMarket().getProxies(), null);
      cookiesMap = POSTFetcher.fetchCookiesWithFetcher(fetcherPayload, session);
    } else {
      cookiesMap = DataFetcher.fetchCookies(session, HOME_PAGE, cookies, userAgent, null, 1);
    }

    for (Entry<String, String> entry : cookiesMap.entrySet()) {
      BasicClientCookie cookie = new BasicClientCookie(entry.getKey(), entry.getValue());
      cookie.setDomain("www.petz.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
    }

  }

  @Override
  protected Object fetch() {
    LettProxy proxy = session.getRequestProxy(HOME_PAGE);
    String page = GETFetcher.fetchPageGET(session, session.getOriginalURL(), cookies, this.userAgent, proxy, 1);

    return Jsoup.parse(page);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      String internalPid = crawlInternalPid(doc);
      CategoryCollection categories = crawlCategories(doc);
      Elements variations = doc.select(".opt_radio_variacao[data-urlvariacao]");

      if (variations.size() > 1) {
        Logging.printLogInfo(logger, session, "Page with more than one product.");
        for (Element e : variations) {
          String nameVariation = crawlNameVariation(e);

          if (e.hasClass("active")) {
            Product p = crawlProduct(doc, nameVariation);
            p.setInternalPid(internalPid);
            p.setCategory1(categories.getCategory(0));
            p.setCategory2(categories.getCategory(1));
            p.setCategory3(categories.getCategory(2));

            products.add(p);
          } else {
            String url = (HOME_PAGE + e.attr("data-urlvariacao")).replace("br//", "br/");
            LettProxy proxy = session.getRequestProxy(HOME_PAGE);
            Document docVariation = Jsoup.parse(GETFetcher.fetchPageGET(session, url, cookies, this.userAgent, proxy, 1));

            Product p = crawlProduct(docVariation, nameVariation);
            p.setInternalPid(internalPid);
            p.setCategory1(categories.getCategory(0));
            p.setCategory2(categories.getCategory(1));
            p.setCategory3(categories.getCategory(2));

            products.add(p);
          }
        }
      } else {
        Logging.printLogInfo(logger, session, "Page with only on product.");
        Product p = crawlProduct(doc, null);
        p.setInternalPid(internalPid);
        p.setCategory1(categories.getCategory(0));
        p.setCategory2(categories.getCategory(1));
        p.setCategory3(categories.getCategory(2));

        products.add(p);
      }

    } else {
      CommonMethods.saveDataToAFile(doc, "/home/gabriel/htmls/PETZ.html");
      Logging.printLogDebug(logger, session, "Not a product page: " + this.session.getOriginalURL());
    }

    return products;
  }

  private Product crawlProduct(Document doc, String nameVariation) {
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String name = crawlName(doc, nameVariation);
      String internalId = crawlInternalId(doc);
      boolean available = doc.select(".is_available").first() != null;
      String description = crawlDescription(doc);
      Float price = crawlPrice(doc);
      Prices prices = crawlPrices(price, doc);
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc, primaryImage);

      return ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setName(name).setPrice(price).setPrices(prices)
          .setAvailable(available).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setMarketplace(new Marketplace()).build();
    }

    return new Product();
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(Document document) {
    return document.select(".prod-info").first() != null;
  }

  /*******************
   * General methods *
   *******************/

  private String crawlNameVariation(Element e) {
    String nameVariation = null;

    Element name = e.select("label > div").first();
    if (name != null) {
      nameVariation = name.ownText().replace("\"", "");
    }

    return nameVariation;
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element sku = doc.select(".prod-info .reset-padding").first();
    if (sku != null) {
      internalId = sku.ownText().replace("\"", "").trim();
    }

    return internalId;
  }

  private String crawlInternalPid(Document doc) {
    String internalPid = null;

    Element pid = doc.select("#prodNotificacao").first();
    if (pid != null) {
      internalPid = pid.val();
    }

    return internalPid;
  }

  private String crawlName(Document doc, String nameVariation) {
    StringBuilder name = new StringBuilder();

    Element nameElement = doc.select("h3[itemprop=name]").first();

    if (nameElement != null) {
      name.append(nameElement.ownText());

      if (nameVariation != null) {
        name.append(" " + nameVariation);
      }
    }

    return name.toString();
  }

  private Float crawlPrice(Document doc) {
    Float price = null;
    Element priceElement = doc.select(".price-current").first();

    if (priceElement != null) {
      price = MathUtils.parseFloat(priceElement.ownText());
    }

    return price;
  }

  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;

    Element image = doc.select(".sp-wrap > a").first();

    if (image != null) {
      primaryImage = image.attr("href");
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document doc, String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements images = doc.select(".sp-wrap > a");

    for (Element e : images) {
      String image = e.attr("href").trim();

      if (!image.equals(primaryImage)) {
        secondaryImagesArray.put(image);
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select("#breadcrumbList li[itemprop] a span");

    for (int i = 1; i < elementCategories.size(); i++) { // first item is the home page
      categories.add(elementCategories.get(i).text().trim());
    }

    return categories;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Element shortDescription = doc.select("p[dir=ltr]").first();
    if (shortDescription != null) {
      description.append(shortDescription.html());
    }

    Elements elementsInformation = doc.select(".infos, #especificacoes, .prodEspecificacoes");
    for (Element e : elementsInformation) {
      if (e.select(".depoimento").isEmpty()) {
        description.append(e.html());
      }
    }

    return description.toString();
  }

  /**
   * To crawl this prices is accessed a api Is removed all accents for crawl price 1x like this: Visa
   * à vista R$ 1.790,00
   * 
   * @param internalId
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      prices.setBankTicketPrice(price);

      Element priceFrom = doc.select(".de-riscado").first();

      if (priceFrom != null) {
        prices.setPriceFrom(MathUtils.normalizeTwoDecimalPlaces(MathUtils.parseFloat(priceFrom.ownText()).doubleValue()));
      }

      Map<Integer, Float> mapInstallments = new HashMap<>();
      mapInstallments.put(1, price);

      prices.insertCardInstallment(Card.VISA.toString(), mapInstallments);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), mapInstallments);
      prices.insertCardInstallment(Card.AMEX.toString(), mapInstallments);
      prices.insertCardInstallment(Card.DINERS.toString(), mapInstallments);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), mapInstallments);
      prices.insertCardInstallment(Card.ELO.toString(), mapInstallments);
      prices.insertCardInstallment(Card.SHOP_CARD.toString(), mapInstallments);
    }

    return prices;
  }
}
