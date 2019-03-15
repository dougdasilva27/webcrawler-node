package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
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
import models.Marketplace;
import models.prices.Prices;


public class BrasilCasaeconstrucaoCrawler extends Crawler {

  public BrasilCasaeconstrucaoCrawler(Session session) {
    super(session);
  }

  private static final String HOME_PAGE = "https://www.cec.com.br/";

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
  }

  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(doc);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1[itemprop=name]", true);
      Float price = CrawlerUtils.scrapSimplePriceFloat(doc, ".product-price .price strong", true);
      Prices prices = crawlPrices(price, doc);
      boolean available = crawlAvailability(doc);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li:not(:first-child) > a", false);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-figure .product-img-zoom img",
          Arrays.asList("data-zoom-image", "src"), "https:", "carrinho.cec.com.br");
      String secondaryImages = crawlSecondaryImages(doc);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#productDetail"));
      Integer stock = null;

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(null).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).setMarketplace(new Marketplace()).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".product-identification").isEmpty();
  }

  private String crawlInternalId(Document document) {
    String internalId = null;

    JSONObject skuJson = CrawlerUtils.selectJsonFromHtml(document, "script", "var google_tag_params = ", ";", false, false);
    if (skuJson.has("ecomm_prodid")) {
      internalId = skuJson.getString("ecomm_prodid");
    }

    return internalId;
  }

  private String crawlSecondaryImages(Document doc) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    JSONArray imagesArray = CrawlerUtils.selectJsonArrayFromHtml(doc, "script", "window['Images']=", ";", true, false);

    for (int i = 1; i < imagesArray.length(); i++) {
      JSONObject obj = imagesArray.getJSONObject(i);

      if (obj.has("Large")) {
        secondaryImagesArray.put(obj.get("Large").toString());
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private boolean crawlAvailability(Document doc) {
    return doc.selectFirst("#btnAddBasket") != null;
  }

  /**
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);

      Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(".product-monthly-payment", doc, false);
      if (!pair.isAnyValueNull()) {
        installmentPriceMap.put(pair.getFirst(), pair.getSecond());
      }

      prices.setBankTicketPrice(price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
    }

    return prices;
  }
}
