package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.Arrays;
import java.util.List;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.B2WCrawler;

public class SaopauloAmericanasCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.americanas.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "americanas.com";

  public SaopauloAmericanasCrawler(Session session) {
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
    List<String> subSellers =
        Arrays.asList("lojas americanas", "lojas americanas mg", "lojas americanas rj", "lojas americanas sp", "lojas americanas rs");
    B2WCrawler b2w = new B2WCrawler(session, logger, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies, subSellers);

    return b2w.extractProducts(doc);
  }
}
