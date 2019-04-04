package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.CompraCertaCrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public class BrasilCompracertaCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.compracerta.com.br/";

  public BrasilCompracertaCrawler(Session session) {
    super(session);
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
      products.addAll(new CompraCertaCrawlerUtils(session, logger, dataFetcher).extractProducts(doc));
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(Document doc) {
    String producReference = crawlProductReference(doc).toLowerCase();
    return !doc.select(".productName").isEmpty() && !producReference.endsWith("_out");
  }

  private String crawlProductReference(Document doc) {
    String producReference = "";
    Element prod = doc.select(".skuReference").first();

    if (prod != null) {
      producReference = prod.ownText().trim();
    }

    return producReference;
  }
}
