package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.LettProxy;
import br.com.lett.crawlernode.core.fetcher.methods.GETFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;
import models.Marketplace;
import models.prices.Prices;

public class BrasilMartinsCrawler extends Crawler {

  private final String HOME_PAGE = "https://b.martins.com.br/";

  public BrasilMartinsCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

//  private String userAgent;
//  private LettProxy proxy;
//
//  @Override
//  public void handleCookiesBeforeFetch() {
//    this.userAgent = DataFetcher.randUserAgent();
//
//    Map<String, String> headers = new HashMap<>();
//    headers.put("Content-Type", "application/x-www-form-urlencoded");
//
//    Map<String, String> cookiesMapHome =
//        POSTFetcher.fetchCookiesPOSTWithHeaders("https://b.martins.com.br/ajax/ajaxCodigoCliente.aspx?mail=victor.fernandes1@br.nestle.com", session,
//            "idemail=victor.fernandes1%40br.nestle.com", cookies, proxy, userAgent, 1, headers);
//
//    for (Entry<String, String> entry : cookiesMapHome.entrySet()) {
//      BasicClientCookie cookie = new BasicClientCookie(entry.getKey(), entry.getValue());
//      cookie.setDomain("b.martins.com.br");
//      cookie.setPath("/");
//      this.cookies.add(cookie);
//    }
//
//    this.proxy = session.getRequestProxy(HOME_PAGE);
//
//    String url = "https://b.martins.com.br/ajax/ajaxLogarUsuario.aspx";
//    Map<String, String> cookiesMap =
//        POSTFetcher.fetchCookiesPOSTWithHeaders(url, session, "e=victor.fernandes1@br.nestle.com&p=nestle@2017&c=4041415&t=0", cookies, 1, headers);
//
//    for (Entry<String, String> entry : cookiesMap.entrySet()) {
//      BasicClientCookie cookie = new BasicClientCookie(entry.getKey(), entry.getValue());
//      cookie.setDomain("b.martins.com.br");
//      cookie.setPath("/");
//      this.cookies.add(cookie);
//    }
//
//  }
//
//  @Override
//  protected Object fetch() {
//    return Jsoup.parse(GETFetcher.fetchPageGET(session, session.getOriginalURL(), cookies, this.userAgent, proxy, 1));
//  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      // InternalId
      String internalId = crawlInternalId(doc);

      // internalPid
      String internalPid = crawlInternalPid(doc);

      // availability
      boolean available = crawlAvailability(doc);

      // name
      String name = crawlName(doc);

      // price
      Float price = crawlPrice(doc);

      // prices
      Prices prices = crawlPrices(doc);

      // categories
      ArrayList<String> categories = crawlCategories(doc);
      String category1 = getCategory(categories, 0);
      String category2 = getCategory(categories, 1);
      String category3 = getCategory(categories, 2);

      // primary image
      String primaryImage = crawlPrimaryImage(doc);

      // secondary images
      String secondaryImages = crawlSecondaryImages(doc);

      // description
      String description = crawlDescription(doc);

      // stock
      Integer stock = null;

      // marketplace
      Marketplace marketplace = new Marketplace();

      Product product = new Product();
      product.setUrl(session.getOriginalURL());
      product.setInternalId(internalId);
      product.setInternalPid(internalPid);
      product.setName(name);
      product.setPrice(price);
      product.setPrices(prices);
      product.setAvailable(available);
      product.setCategory1(category1);
      product.setCategory2(category2);
      product.setCategory3(category3);
      product.setPrimaryImage(primaryImage);
      product.setSecondaryImages(secondaryImages);
      product.setDescription(description);
      product.setStock(stock);
      product.setMarketplace(marketplace);

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }

  private boolean isProductPage(String originalURL) {
    return (originalURL.startsWith("https://b.martins.com.br/detalhe_produto"));
  }

  private String crawlInternalPid(Document doc) {
    return null;
  }

  private Prices crawlPrices(Document doc) {
    return new Prices();
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Element infoProd = doc.select("#ctnCodProduto").first();
    if (infoProd != null) {
      description.append(infoProd.html());
    }

    Element saleArgumentElement = doc.select("#lblArgumentoVenda").first();
    if (saleArgumentElement != null) {
      description.append(saleArgumentElement.html());
    }

    Element bodyPostersElement = doc.select("div#corpo").first();
    if (bodyPostersElement != null) {
      description.append(bodyPostersElement.html());
    }

    Element moreInformationElementTab2 = doc.select("#ctnMaisInfo #dvFieldset #tab2 .ctnZebraListaBranca").first();
    if (moreInformationElementTab2 != null) {
      description.append(moreInformationElementTab2.html());
    }

    return description.toString();
  }

  private String crawlSecondaryImages(Document doc) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements secondaryImagesElements = doc.select("#slider .thumbnail > a");
    for (int i = 1; i < secondaryImagesElements.size(); i++) { // the first is the same as the primary image
      secondaryImagesArray.put(secondaryImagesElements.get(i).attr("href").trim());
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;
    Element primaryImageElement = doc.select("#imgPrincipalProduto a").first();
    if (primaryImageElement != null) {
      primaryImage = primaryImageElement.attr("href").trim();
    }
    return primaryImage;
  }

  private ArrayList<String> crawlCategories(Document doc) {
    ArrayList<String> categories = new ArrayList<String>();

    Elements categoriesElements = doc.select("#ctnCaminhoMigalhas li");
    for (int i = 1; i < categoriesElements.size(); i++) { // first one is a link for home
      Element hrefElement = categoriesElements.get(i).select("a").first();
      if (hrefElement != null) {
        categories.add(hrefElement.text().trim());
      }
    }

    return categories;
  }

  private Float crawlPrice(Document doc) {
    Float price = null;

    Element elementPrice = doc.select(".ctnValorUnitario span").first();
    if (elementPrice != null) {
      price = MathCommonsMethods.parseFloat(elementPrice.text());
    }

    return price;
  }

  private String crawlName(Document doc) {
    String name = null;
    Element nameElement = doc.select("#ctnTitProduto h2").first();
    if (nameElement != null) {
      name = nameElement.text().trim();
    }
    return name;
  }

  private boolean crawlAvailability(Document doc) {
    return false;
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;
    Element internalIdElement = doc.select("#ctnCodProduto").first();
    if (internalIdElement != null) {
      List<TextNode> textNodes = internalIdElement.textNodes();
      if (!textNodes.isEmpty()) {
        String internalIdText = textNodes.get(0).text().trim();
        List<String> parsedNumbers = MathCommonsMethods.parseNumbers(internalIdText);
        if (!parsedNumbers.isEmpty()) {
          internalId = parsedNumbers.get(0);
        }
      }
    }
    return internalId;
  }

  private String getCategory(ArrayList<String> categories, int n) {
    if (n < categories.size()) {
      return categories.get(n);
    }

    return "";
  }

}
