package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
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

  // private String userAgent;
  // private LettProxy proxy;
  //
  // @Override
  // public void handleCookiesBeforeFetch() {
  // this.userAgent = DataFetcher.randUserAgent();
  //
  // Map<String, String> headers = new HashMap<>();
  // headers.put("Content-Type", "application/x-www-form-urlencoded");
  //
  // Map<String, String> cookiesMapHome =
  // POSTFetcher.fetchCookiesPOSTWithHeaders("https://b.martins.com.br/ajax/ajaxCodigoCliente.aspx?mail=victor.fernandes1@br.nestle.com",
  // session,
  // "idemail=victor.fernandes1%40br.nestle.com", cookies, proxy, userAgent, 1, headers);
  //
  // for (Entry<String, String> entry : cookiesMapHome.entrySet()) {
  // BasicClientCookie cookie = new BasicClientCookie(entry.getKey(), entry.getValue());
  // cookie.setDomain("b.martins.com.br");
  // cookie.setPath("/");
  // this.cookies.add(cookie);
  // }
  //
  // this.proxy = session.getRequestProxy(HOME_PAGE);
  //
  // String url = "https://b.martins.com.br/ajax/ajaxLogarUsuario.aspx";
  // Map<String, String> cookiesMap =
  // POSTFetcher.fetchCookiesPOSTWithHeaders(url, session,
  // "e=victor.fernandes1@br.nestle.com&p=nestle@2017&c=4041415&t=0", cookies, 1, headers);
  //
  // for (Entry<String, String> entry : cookiesMap.entrySet()) {
  // BasicClientCookie cookie = new BasicClientCookie(entry.getKey(), entry.getValue());
  // cookie.setDomain("b.martins.com.br");
  // cookie.setPath("/");
  // this.cookies.add(cookie);
  // }
  //
  // }
  //
  // @Override
  // protected Object fetch() {
  // return Jsoup.parse(GETFetcher.fetchPageGET(session, session.getOriginalURL(), cookies,
  // this.userAgent, proxy, 1));
  // }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(doc);
      String name = crawlName(doc);
      Float price = crawlPrice(doc);

      ArrayList<String> categories = crawlCategories(doc);
      String category1 = getCategory(categories, 0);
      String category2 = getCategory(categories, 1);
      String category3 = getCategory(categories, 2);

      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc);
      String description = crawlDescription(doc);

      // ean
      String ean = crawlEan(doc);

      List<String> eans = new ArrayList<>();
      eans.add(ean);

      Product product = new Product();
      product.setUrl(session.getOriginalURL());
      product.setInternalId(internalId);
      product.setName(name);
      product.setPrice(price);
      product.setPrices(new Prices());
      product.setAvailable(false);
      product.setCategory1(category1);
      product.setCategory2(category2);
      product.setCategory3(category3);
      product.setPrimaryImage(primaryImage);
      product.setSecondaryImages(secondaryImages);
      product.setDescription(description);
      product.setMarketplace(new Marketplace());
      product.setEans(eans);

      products.add(product);

    } else {
      products.addAll(extractProductsNewWay(doc));
    }

    return products;
  }

  private List<Product> extractProductsNewWay(Document doc) {
    List<Product> products = new ArrayList<>();

    if (!doc.select("input#id").isEmpty()) {
      String internalId = CommonMethods.getLast(CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input#id", "value").split("_"));
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".qdDetails .title", true);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li:not(:first-child) > a", true);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".imagePrincipal img", Arrays.asList("src"), "https", "imgprd.martins.com.br");
      String secondaryImages =
          CrawlerUtils.scrapSimpleSecondaryImages(doc, ".galeryImages img", Arrays.asList("src"), "https", "imgprd.martins.com.br", primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".qdDetails .cods", ".details", "#especfication"));
      List<String> eans = Arrays.asList(CrawlerUtils.scrapStringSimpleInfo(doc, ".cods .col-2 p", true));

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setName(name).setPrices(new Prices())
          .setAvailable(false).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2))
          .setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description).setMarketplace(new Marketplace())
          .setEans(eans).build();

      products.add(product);
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private boolean isProductPage(Document doc) {
    return !doc.select("#ctnTitProduto").isEmpty();
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
    for (int i = 1; i < secondaryImagesElements.size(); i++) { // the first is the same as the
                                                               // primary image
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
      price = MathUtils.parseFloatWithComma(elementPrice.text());
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

  private String crawlInternalId(Document doc) {
    String internalId = null;
    Element internalIdElement = doc.select("#ctnCodProduto").first();
    if (internalIdElement != null) {
      List<TextNode> textNodes = internalIdElement.textNodes();
      if (!textNodes.isEmpty()) {
        String internalIdText = textNodes.get(0).text().trim();
        List<String> parsedNumbers = MathUtils.parseNumbers(internalIdText);
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

  private String crawlEan(Document doc) {
    String ean = null;
    Element e = doc.selectFirst("#ctnDesDetalheProduto #quick-codigo-barras");

    if (e != null) {
      ean = e.attr("value");
    }

    return ean;
  }
}
