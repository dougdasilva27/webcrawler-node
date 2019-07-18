package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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

public class BrasilSempreemcasaCrawler extends Crawler {

  public BrasilSempreemcasaCrawler(Session session) {
    super(session);
  }

  private static final String IMAGES_HOST = "cdn.shopify.com";

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    Element productItem = doc.selectFirst(".product-item");

    if (productItem != null) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      CategoryCollection categories = new CategoryCollection();
      String primaryImage =
          CrawlerUtils.scrapSimplePrimaryImage(productItem, ".product-item__img img", Arrays.asList("data-src"), "https", IMAGES_HOST);
      String name = CrawlerUtils.scrapStringSimpleInfo(productItem, ".product-item__title-text", false);

      Elements variations = productItem.select(".product-item__variants-item[data-variant]");
      for (Element e : variations) {
        String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, null, "data-id");
        String internalId = e.attr("data-variant");

        // In this market all products are sold per pack, so variations always have "unidades"
        String nameVariation = name.concat(" ").concat(e.ownText().trim()).concat(" unidades");

        Prices prices = crawlPrices(doc, internalId);
        Float price = CrawlerUtils.extractPriceFromPrices(prices, Card.MASTERCARD);

        // In this market was not found unavailable products
        boolean available = true;

        // Creating the product
        Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId).setInternalPid(internalPid)
            .setName(nameVariation)
            .setPrice(price)
            .setPrices(prices)
            .setAvailable(available)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setMarketplace(new Marketplace())
            .build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  /**
   * @param doc
   * @param internalId
   * @return
   */
  private Prices crawlPrices(Element doc, String internalId) {
    Prices prices = new Prices();

    Float price = CrawlerUtils.scrapFloatPriceFromHtml(
        doc, ".product-item__variant-data[data-variant=" + internalId + "] .price__price", null, true, ',', session);

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);

      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
    }

    return prices;
  }
}
