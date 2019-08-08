package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public class SaopauloUltrafarmaCrawler extends Crawler {

  private static final String HOME_PAGE = "http://www.ultrafarma.com.br/";

  public SaopauloUltrafarmaCrawler(Session session) {
    super(session);
    // super.config.setFetcher(FetchMode.WEBDRIVER);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  // @Override
  // protected Document fetch() {
  // Document doc = new Document("");
  // this.webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), session);
  //
  // if (this.webdriver != null) {
  // doc = Jsoup.parse(this.webdriver.getCurrentPageSource());
  //
  // Element script = doc.select("head script").last();
  // Element robots = doc.select("meta[name=robots]").first();
  //
  // if (script != null && robots != null) {
  // String eval = script.html().trim();
  //
  // if (!eval.isEmpty()) {
  // Logging.printLogDebug(logger, session, "Execution of incapsula js script...");
  // this.webdriver.executeJavascript(eval);
  // }
  // }
  //
  // String requestHash = FetchUtilities.generateRequestHash(session);
  // this.webdriver.waitLoad(12000);
  //
  // doc = Jsoup.parse(this.webdriver.getCurrentPageSource());
  // Logging.printLogDebug(logger, session, "Terminating PhantomJS instance ...");
  // this.webdriver.terminate();
  //
  // // saving request content result on Amazon
  // S3Service.saveResponseContent(session, requestHash, doc.toString());
  // }
  //
  // return doc;
  // }

  @Override
  public void handleCookiesBeforeFetch() {
    Logging.printLogDebug(logger, session, "Adding cookie...");

    BasicClientCookie cookie = new BasicClientCookie("ultrafarma_uf", "SP");
    cookie.setDomain(".ultrafarma.com.br");
    cookie.setPath("/");
    this.cookies.add(cookie);
  }


  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".skuReference", true);
      String internalPid = internalId;

      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.product-name", true);
      String description = crawlDescription(doc);
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".product-price-new span[data-preco]", null, true, ',', session);
      Prices prices = crawlPrices(doc, price);
      boolean available = !doc.select(".product-stock").isEmpty();
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li:not(:first-child):not(.active) a", true);

      JSONArray images = CrawlerUtils.selectJsonArrayFromHtml(doc, "script", "LeanEcommerce.PDP_PRODUTO_IMAGENS=JSON.parse('", "')", true, false);
      String primaryImage = scrapPrimaryImage(images);
      String secondaryImages = scrapSecondaryImages(images);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setMarketplace(new Marketplace()).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }


  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();
    String productDetails = null;
    description.append(CrawlerUtils.scrapSimpleDescription(doc,
        Arrays.asList(".product-references .product-seller-brand-name",
            "#pdp-section-outras-informacoes",
            ".product-details-section[id~=anvisa]")));

    Element productDetailsElement = doc.selectFirst(".product-details-container .product-details-section:not([ng-if]):not([id])");

    if (productDetailsElement != null) {
      productDetails = productDetailsElement.text();
      if (!productDetails.contains("avaliar")) {
        description.append(productDetails);
      }
    }

    return description.toString();
  }

  private String scrapPrimaryImage(JSONArray images) {
    String primaryImage = null;

    for (Object o : images) {
      JSONObject json = (JSONObject) o;

      if (json.has("Principal") && !json.isNull("Principal") && json.getBoolean("Principal")) {

        String key = null;
        if (json.has("Grande") && !json.isNull("Grande")) {
          key = "Grande";
        } else if (json.has("Media") && !json.isNull("Media")) {
          key = "Media";
        } else if (json.has("Pequena") && !json.isNull("Pequena")) {
          key = "Pequena";
        } else if (json.has("Mini") && !json.isNull("Mini")) {
          key = "Mini";
        }

        if (key != null) {
          primaryImage = CrawlerUtils.completeUrl(json.get(key).toString(), "https", "ultrafarma-storage.azureedge.net");
        }

        break;
      }
    }

    return primaryImage;
  }

  private String scrapSecondaryImages(JSONArray images) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    for (Object o : images) {
      JSONObject json = (JSONObject) o;

      if (json.has("Principal") && !json.isNull("Principal") && !json.getBoolean("Principal")) {

        String key = null;
        if (json.has("Grande") && !json.isNull("Grande")) {
          key = "Grande";
        } else if (json.has("Media") && !json.isNull("Media")) {
          key = "Media";
        } else if (json.has("Pequena") && !json.isNull("Pequena")) {
          key = "Pequena";
        } else if (json.has("Mini") && !json.isNull("Mini")) {
          key = "Mini";
        }

        if (key != null) {
          secondaryImagesArray.put(CrawlerUtils.completeUrl(json.get(key).toString(), "https", "ultrafarma-storage.azureedge.net"));
        }
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();
    Map<Integer, Float> installmentPriceMap = new HashMap<>();

    if (price != null) {
      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);
      prices.setPriceFrom(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-price-old span[data-preco]", null, true, ',', session));

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
    }

    return prices;
  }


  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".product") != null;
  }
}
