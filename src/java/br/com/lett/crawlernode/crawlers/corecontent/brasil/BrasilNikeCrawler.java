package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.Pair;
import models.prices.Prices;

public class BrasilNikeCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.nike.com.br";
  private final Map<String, String> defaultHeaders;

  public BrasilNikeCrawler(Session session) {
    super(session);

    defaultHeaders = new HashMap<>();
    defaultHeaders.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
    defaultHeaders.put("accept-encoding", "gzip, deflate, br");
    defaultHeaders.put("accept-language", "en-US,en;q=0.9");
    defaultHeaders.put("cache-control", "max-age=0");
    defaultHeaders.put("upgrade-insecure-requests", "1");
    defaultHeaders.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36");
  }

  @Override
  protected Object fetch() {
    try {
      return Jsoup.connect(session.getOriginalURL()).headers(defaultHeaders).get();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
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

      JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"text/javascript\"]", "window.siteMetadata = ", ";", false, true);
      json = json.has("page") ? json.getJSONObject("page") : new JSONObject();
      json = json.has("product") ? json.getJSONObject("product") : new JSONObject();

      String internalId = json.has("idSku") ? json.getString("idSku") : null;
      String internalPid = json.has("idProduct") ? json.getString("idProduct") : null;;
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".produtoNome h1.name", true);
      CategoryCollection categories = new CategoryCollection();
      String primaryImage = json.has("image") ? json.getString("image") : null;
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".prod-midia .box-thumbs-img .thumbs-img .it-thumbs-img a",
          Arrays.asList("href"), "https:", "assets.nike.com.br", primaryImage);
      String description = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-description .feats .details", false);
      Integer stock = null;
      Float price = scrapPrice(json);
      Prices prices = scrapPrices(doc, price);
      boolean available = false;

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".productDetails strong[id]") != null;
  }

  private Float scrapPrice(JSONObject json) {
    Float price = null;

    price = json.has("salePrice") ? json.getFloat("salePrice") : null;

    if (price == null) {
      price = json.has("basePrice") ? json.getFloat("basePrice") : null;
    }

    return price;
  }

  private Prices scrapPrices(Document doc, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Double priceFrom = CrawlerUtils.scrapSimplePriceDouble(doc, ".descricaoAnuncio .productDetails strong[id~=PrecoDe]", false);
      prices.setPriceFrom(priceFrom);
      prices.setBankTicketPrice(price);

      Pair<Integer, Float> par = CrawlerUtils.crawlSimpleInstallment(".descricaoAnuncio .productDetails .parcel", doc, false, "de", "", true);

      if (!par.isAnyValueNull()) {
        Map<Integer, Float> installmentPriceMap = new TreeMap<>();
        installmentPriceMap.put(1, price);
        installmentPriceMap.put(par.getFirst(), par.getSecond());

        prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      }
    }

    return prices;
  }
}
