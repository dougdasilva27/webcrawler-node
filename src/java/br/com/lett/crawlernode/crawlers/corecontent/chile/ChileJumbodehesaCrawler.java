package br.com.lett.crawlernode.crawlers.corecontent.chile;

import java.util.List;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.ChileJumboCrawler;
import br.com.lett.crawlernode.util.Logging;

public class ChileJumbodehesaCrawler extends Crawler {

  private static final String HOME_PAGE = "https://nuevo.jumbo.cl/";

  public ChileJumbodehesaCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public void handleCookiesBeforeFetch() {
    Logging.printLogDebug(logger, session, "Adding cookie...");

    BasicClientCookie cookie = new BasicClientCookie("VTEXSC", "sc=" + ChileJumboCrawler.JUMBO_DEHESA_ID);
    cookie.setDomain(".nuevo.jumbo.cl");
    cookie.setPath("/");
    this.cookies.add(cookie);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);

    return new ChileJumboCrawler(session, cookies, logger).extractProducts(doc);
  }

}
