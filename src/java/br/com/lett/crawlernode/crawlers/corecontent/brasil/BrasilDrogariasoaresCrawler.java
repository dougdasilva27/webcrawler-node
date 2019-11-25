package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

/**
 * 
 * @author gabriel date: 2019-10-15
 */
public class BrasilDrogariasoaresCrawler extends Crawler {

  public BrasilDrogariasoaresCrawler(Session session) {
    super(session);
  }

  private static final String HOME_PAGE = "https://www.drogariasoares.com.br/";

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
  }


  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#adicionar-lista[data-compra]", "data-compra");
      String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, "#side-prod h4 small", true);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "#side-prod h2", true);
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".produto-disponivel .preco-por strong", null, true, ',', session);
      boolean available = !doc.select(".produto-disponivel.show").isEmpty();
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs li.show-for-large-up:not(.current) a");
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#gallery li a", Arrays.asList("data-zoom-image", "data-image"), "https",
          "www.drogariasoares.com.br");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#gallery li a", Arrays.asList("data-zoom-image", "data-image"), "https",
          "www.drogariasoares.com.br", primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#corpo-detalhe .prod-descr .prod-content-left"));
      Prices prices = crawlPrices(price, doc);

      // Creating the product
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
          .setMarketplace(new Marketplace())
          .build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst("#adicionar-lista[data-compra]") != null;
  }

  /**
   * 
   * @param price
   * @param doc
   * @return
   */
  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();
      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);

      prices.setPriceFrom(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".preco-det .preco-de", null, true, ',', session));

      Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(".preco-det .preco-parcela", doc, false, "x");
      if (!pair.isAnyValueNull()) {
        installmentPriceMap.put(pair.getFirst(), pair.getSecond());
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
    }

    return prices;
  }
}
