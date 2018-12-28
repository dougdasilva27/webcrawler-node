package br.com.lett.crawlernode.crawlers.corecontent.peru;

import java.util.List;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.TottusCrawler;

/**
 * Date: 17/12/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class PeruTottusCrawler extends Crawler {

  private static final String HOME_PAGE = "http://www.tottus.com.pe/";

  public PeruTottusCrawler(Session session) {
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
    TottusCrawler t = new TottusCrawler(logger, session, false);

    return t.extractInformation(doc);
  }
}
