package br.com.lett.crawlernode.crawlers.corecontent.chile;

import java.util.List;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.TottusCrawler;

/**
 * Date: 03/12/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class ChileTottusCrawler extends Crawler {

  private static final String HOME_PAGE = "http://www.tottus.cl/";

  public ChileTottusCrawler(Session session) {
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
    TottusCrawler t = new TottusCrawler(logger, session, true);

    return t.extractInformation(doc);
  }
}
