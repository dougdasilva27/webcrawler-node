package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.List;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.B2WCrawler;

public class SaopauloShoptimeCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.shoptime.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "shoptime";

  public SaopauloShoptimeCrawler(Session session) {
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
    B2WCrawler b2w = new B2WCrawler(session, logger, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies, new ArrayList<>());

    return b2w.extractProducts(doc);
  }
}
