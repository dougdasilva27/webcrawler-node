package br.com.lett.crawlernode.crawlers.corecontent.chile;

import java.util.List;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.CornershopCrawler;

public class ChileCornershoplidervitacuraCrawler extends Crawler {

  private static final String HOME_PAGE = "https://web.cornershopapp.com";
  public static final String STORE_ID = "19";

  private CornershopCrawler cornerCrawler = new CornershopCrawler(session, STORE_ID, logger, cookies);

  public ChileCornershoplidervitacuraCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  protected Object fetch() {
    return cornerCrawler.fetch();
  }

  @Override
  public List<Product> extractInformation(JSONObject jsonSku) throws Exception {
    super.extractInformation(jsonSku);

    return cornerCrawler.extractInformation(jsonSku);
  }
}
