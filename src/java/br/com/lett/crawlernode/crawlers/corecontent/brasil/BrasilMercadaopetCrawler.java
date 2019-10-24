package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.prices.Prices;

public class BrasilMercadaopetCrawler extends Crawler {

  public BrasilMercadaopetCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    if (isProductPage(doc)) {

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".single-product-info .add_to_wishlist[data-product-id]",
          "data-product-id");
      String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, ".product_meta .sku", true);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.product_title", true);
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".product-content .price", null, false, ',', session);
      Prices prices = scrapPrices(price);
      boolean available = doc.selectFirst(".stock.in-stock") != null;
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs-inner a:not(:first-child)");
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".images a", Arrays.asList("href"), "https",
          "mercadaopet.com.br");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".images a", Arrays.asList("href"), "https",
          "mercadaopet.com.br", primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".short-description", "#tab-description",
          "#tab-additional_information"));

      Product product = ProductBuilder.create()
          .setUrl(session.getOriginalURL())
          .setInternalId(internalId)
          .setInternalPid(internalPid)
          .setName(name)
          .setPrice(price)
          .setPrices(prices)
          .setAvailable(available)
          .setCategory1(categories.getCategory(0))
          .setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2))
          .setPrimaryImage(primaryImage)
          .setSecondaryImages(secondaryImages)
          .setDescription(description)
          .build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private Prices scrapPrices(Float price) {
    Prices prices = new Prices();
    if (price != null) {

      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      prices.setBankTicketPrice(price);
      installmentPriceMap.put(1, price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);

    }
    return prices;
  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".product_meta .sku").isEmpty();
  }
}
