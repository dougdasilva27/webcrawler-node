package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public class BrasilLojamondelezCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.lojamondelez.com.br/";

  public BrasilLojamondelezCrawler(Session session) {
    super(session);
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

    if (!doc.select(".infoProduct").isEmpty()) {
      String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-ean .value", true);
      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".infoProduct [data-product-id]", "data-product-id");
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.nameProduct", true);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "a.thumb-link", Arrays.asList("data-zoom-image", "data-img-medium", "href"),
          "https", "i1-mondelez.a8e.net.br");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "a.thumb-link", Arrays.asList("data-zoom-image", "data-img-medium",
          "href"), "https", "i1-mondelez.a8e.net.br", primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".description-product"));
      List<String> eans = Arrays.asList(internalId);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".product-breadcrumb a[href]:not(:first-child)");

      // Creating the product
      Product product = ProductBuilder.create()
          .setUrl(session.getOriginalURL())
          .setInternalId(internalId)
          .setInternalPid(internalPid)
          .setName(name)
          .setPrices(new Prices())
          .setAvailable(false)
          .setCategory1(categories.getCategory(0))
          .setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2))
          .setPrimaryImage(primaryImage)
          .setSecondaryImages(secondaryImages)
          .setDescription(description)
          .setMarketplace(new Marketplace())
          .setEans(eans)
          .build();

      products.add(product);
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }
}
