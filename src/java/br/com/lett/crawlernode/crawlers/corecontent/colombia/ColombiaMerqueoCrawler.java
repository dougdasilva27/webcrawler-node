package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import models.Marketplace;
import models.prices.Prices;

public class ColombiaMerqueoCrawler extends Crawler {

  public ColombiaMerqueoCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(doc);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.product-title", true);
      name += (" " + CrawlerUtils.scrapStringSimpleInfo(doc, "h3.product-quantity", true)).trim();
      Float price = CrawlerUtils.scrapSimplePriceFloat(doc, ".product-price.product-special-price, .product-price:not(.text-strike)", false);
      boolean available = doc.select("#modal-add-button") != null;
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb [itemprop=item]");
      Prices prices = crawlPrices(price, doc);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "img.modal-product-image", Arrays.asList("data-zoom-image", "src"), "https:",
          "d50xhnwqnrbqk.cloudfront.net");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "img.modal-product-image", Arrays.asList("data-zoom-image", "src"),
          "https:", "d50xhnwqnrbqk.cloudfront.net", primaryImage);
      String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".product-description"));

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setName(name).setPrice(price)
          .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setMarketplace(new Marketplace()).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return !doc.select("h1.product-title").isEmpty();
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    JSONObject data = CrawlerUtils.selectJsonFromHtml(doc, "script[type=text/javascript]", "vardata=", ";", true, false);
    if (data.has("storeProductId")) {
      internalId = data.get("storeProductId").toString();
    }

    return internalId;
  }

  /**
   * In the time when this crawler was made, this market hasn't installments informations
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      prices.setPriceFrom(CrawlerUtils.scrapSimplePriceDouble(doc, ".product-price.text-strike", false));

      Map<Integer, Float> installmentPriceMapShop = new HashMap<>();
      installmentPriceMapShop.put(1, price);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMapShop);
    }

    return prices;
  }


}
