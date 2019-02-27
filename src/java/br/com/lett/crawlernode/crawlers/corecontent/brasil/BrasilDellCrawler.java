package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import models.prices.Prices;

/**
 * Date: 13/08/2018
 * 
 * @author victor
 *
 */
public class BrasilDellCrawler extends Crawler {


  private static final String HOME_PAGE = "https://www.dell.com/pt-br";

  public BrasilDellCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    List<Product> products = new ArrayList<>();

    JSONObject infoJson = CrawlerUtils.selectJsonFromHtml(doc, "script", "Dell.Services.DataModel=", "};", true, false);

    if (infoJson.has("ProductMicroItems")) {
      JSONArray productsArray = infoJson.getJSONArray("ProductMicroItems");
      boolean hasVariations = productsArray.length() > 1;

      for (Object o : productsArray) {
        JSONObject productJson = (JSONObject) o;

        if (productJson.has("Mpn") && productJson.has("Url")) {
          String internalId = productJson.get("Mpn").toString();
          String newUrl = CrawlerUtils.completeUrl(productJson.get("Url").toString(), "https", "www.dell.com");
          Document newDoc = hasVariations ? DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, newUrl, null, cookies) : doc;

          products.add(extractProduct(newDoc, internalId, newUrl));
        }
      }
    } else {
      Logging.printLogDebug(logger, session, "Not a product page: " + this.session.getOriginalURL());
    }

    return products;
  }


  public Product extractProduct(Document doc, String internalId, String url) throws Exception {
    super.extractInformation(doc);
    Product product = new Product();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "#sharedPdPageProductTitle", true);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs > ol > li:not(:first-child) > a");
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".slides img, #product-detail-feature-container img",
          Arrays.asList("data-blzsrc", "data-original", "src"), "https", "i.dell.com");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".slides img", Arrays.asList("data-blzsrc", "data-original", "src"),
          "https", "i.dell.com", primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc,
          Arrays.asList("div.xs-text-centered > p", "span.marketing-blurb", "#overview> div.xs-top-offset-medium > div.row.xs-pad-offset-15",
              "#product-detail-feature-container > div.md-pad-right-30 > div.row.xs-pad-offset-15"));
      Float price = crawlPrice(doc);
      Prices prices = crawlPrices(doc, price);
      boolean available = price != null;

      product = ProductBuilder.create().setUrl(url).setInternalId(internalId).setName(name).setPrice(price).setPrices(prices).setAvailable(available)
          .setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2))
          .setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description).build();

    } else {
      Logging.printLogDebug(logger, session, "Not a product page: " + this.session.getOriginalURL());
    }
    return product;
  }

  /**
   * Checks if the page acessed is product page or not.
   * 
   * @param doc - contais html from the page to be scrapped
   * @return true if its a skupage.
   */
  private boolean isProductPage(Document doc) {
    return doc.selectFirst("meta[content=productdetails]") != null;
  }

  /**
   * Gets the SKU prices
   * 
   * @param doc - html to be scrapped
   * @return the price scrapped from the sku page
   */
  private Float crawlPrice(Document doc) {
    Float price = null;

    Element priceElement = doc.selectFirst("div.uDetailedPrice > div > div > div > div.vertical-overflow > h5 > strong > span");
    Element specialPriceElement = doc.selectFirst("div.dellPricing > h5 > strong > span.pull-right > span"); // caso a pagina do SKU seja diferente

    if (priceElement != null) {
      price = MathUtils.parseFloatWithComma(priceElement.text());
    } else if (specialPriceElement != null) {
      price = MathUtils.parseFloatWithComma(specialPriceElement.text());
    }

    return price;
  }

  /**
   * Get the SKU Old price if it exists
   * 
   * @param doc - html to be scrapped
   * @return
   */
  private Double crawlOldPrice(Document doc) {
    Double price = null;

    Element priceDiv = doc.selectFirst("div.dell-pricing-total-savings-section > p > span > small");
    if (priceDiv != null) {
      price = MathUtils.parseDoubleWithComma(priceDiv.text());
    }

    return price;
  }

  /**
   * Create a map of Prices and payment way
   * 
   * @param doc - html to be scrapped
   * @param price - price of the SKU
   * @return
   */
  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> paymentPriceMap = new TreeMap<>();

      paymentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);
      prices.setPriceFrom(crawlOldPrice(doc));

      Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment("div.lease-rate-adjusted-amount-per-pay-period > div > p", doc, false);
      if (!pair.isAnyValueNull()) {
        paymentPriceMap.put(pair.getFirst(), pair.getSecond());
      }

      prices.insertCardInstallment(Card.MASTERCARD.toString(), paymentPriceMap);
      prices.insertCardInstallment(Card.VISA.toString(), paymentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), paymentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), paymentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), paymentPriceMap);

    }
    return prices;
  }

}
