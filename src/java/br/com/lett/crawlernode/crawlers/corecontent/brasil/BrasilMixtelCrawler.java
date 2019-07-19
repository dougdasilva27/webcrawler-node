package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public class BrasilMixtelCrawler extends Crawler {

  public BrasilMixtelCrawler(Session session) {
    super(session);
  }


  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {

      boolean available = false;
      String internalId = scrapInternalId(doc);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product_title", false);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".sow-image-container img", Arrays.asList("src"), "https",
          "mixteldistribuidora.com.br");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".sow-image-container img", Arrays.asList("src"), "https",
          "mixteldistribuidora.com.br",
          primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".textwidget",
          ".widget_custom_html.panel-last-child"));

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setName(name)
          .setAvailable(available).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private String scrapInternalId(Document doc) {
    Element internalIdElement = doc.selectFirst(".product.type-product");
    String internalId = null;

    if (internalIdElement != null) {
      internalId = CommonMethods.getLast(internalIdElement.attr("id").split("-"));
    }

    return internalId;
  }


  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".product_title") != null;
  }

}
