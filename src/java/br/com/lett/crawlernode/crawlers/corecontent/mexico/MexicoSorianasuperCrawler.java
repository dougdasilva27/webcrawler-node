package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
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

/**
 * 
 * 1) Only one sku per page.
 * 
 * Price crawling notes: 1) This market needs some cookies for request product pages. 2) There is no
 * bank slip (boleto bancario) payment option. 3) There is no installments for card payment. So we
 * only have 1x payment, and to this value we use the cash price crawled from the sku page. (nao
 * existe divisao no cartao de credito).
 * 
 * @author Gabriel Dornelas
 *
 */
public class MexicoSorianasuperCrawler extends Crawler {

  private static final String DOMAIN = "superentucasa.soriana.com";
  private static final String HOME_PAGE = "https://superentucasa.soriana.com";

  public MexicoSorianasuperCrawler(Session session) {
    super(session);
    super.config.setFetcher(FetchMode.APACHE);
  }

  @Override
  public void handleCookiesBeforeFetch() {
    Request request = RequestBuilder.create().setCookies(cookies).setUrl(HOME_PAGE + "/default.aspx")
        .setFollowRedirects(false).build();

    this.cookies = CrawlerUtils.fetchCookiesFromAPage(request, DOMAIN, "/", null, session, dataFetcher);
  }

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(doc);
      String internalPid = crawlInternalPid(doc);
      String name = crawlName(doc);
      Float price = crawlPrice(doc);
      Prices prices = crawlPrices(price);
      boolean available = crawlAvailability(doc);
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc);
      String description = crawlDescription(doc);
      Integer stock = null;
      Marketplace marketplace = crawlMarketplace(doc);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).setMarketplace(marketplace).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    if (doc.select("#DivDesc").first() != null) {
      return true;
    }
    return false;
  }

  private String crawlInternalId(Document document) {
    String internalId = null;

    Element internalIdElement = document.select("form[target=EditarCarrito] input[name=s]").first();
    if (internalIdElement != null) {
      internalId = internalIdElement.val();
    }

    return internalId;
  }

  /**
   * There is no internalPid.
   * 
   * @param document
   * @return
   */
  private String crawlInternalPid(Document document) {
    String internalPid = null;

    return internalPid;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select("#DivDesc").first();

    if (nameElement != null) {
      name = nameElement.text().trim();
    }

    return name;
  }

  private Float crawlPrice(Document document) {
    Float price = null;

    String priceText = null;
    Element salePriceElement = document.select("#DivPrecio .precioarticulo").first();

    if (salePriceElement != null) {
      priceText = salePriceElement.ownText();
    }

    if (priceText != null && !priceText.isEmpty()) {
      price = Float.parseFloat(priceText.replaceAll(MathUtils.PRICE_REGEX, ""));
    }

    return price;
  }

  private boolean crawlAvailability(Document document) {
    boolean available = false;

    Element outOfStockElement = document.select("#DivObsart textarea").first();
    if (outOfStockElement != null) {
      available = true;
    }

    return available;
  }

  private Marketplace crawlMarketplace(Document document) {
    return new Marketplace();
  }



  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Element primaryImageElement = document.select("#ImgArt img").first();

    if (primaryImageElement != null) {
      primaryImage = primaryImageElement.attr("src").trim();
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document document) {
    return null;
  }

  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    return categories;
  }

  private String crawlDescription(Document document) {
    StringBuilder description = new StringBuilder();

    Elements activeSubstancesElements = document.select("div.DivSustanciasActivas");
    if (!activeSubstancesElements.isEmpty())
      description.append(activeSubstancesElements.html());

    return description.toString();
  }

  /**
   * There is no bankSlip price.
   * 
   * There is no card payment options, other than cash price. So for installments, we will have only
   * one installment for each card brand, and it will be equals to the price crawled on the sku main
   * page.
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();

      installmentPriceMap.put(1, price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
    }

    return prices;
  }

}
