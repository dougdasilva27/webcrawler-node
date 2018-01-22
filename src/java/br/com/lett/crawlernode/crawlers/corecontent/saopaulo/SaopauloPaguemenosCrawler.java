package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.List;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.PaguemenosCrawler;

public class SaopauloPaguemenosCrawler extends Crawler {
  private static final String HOME_PAGE = "https://www.paguemenos.com.br/";

  public SaopauloPaguemenosCrawler(Session session) {
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

    return PaguemenosCrawler.extractInformation(doc, logger, session);
  }
}
