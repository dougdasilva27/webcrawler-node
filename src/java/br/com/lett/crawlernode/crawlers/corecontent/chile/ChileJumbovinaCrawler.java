package br.com.lett.crawlernode.crawlers.corecontent.chile;

import org.apache.http.impl.cookie.BasicClientCookie;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.ChileJumboCrawler;
import br.com.lett.crawlernode.util.Logging;

public class ChileJumbovinaCrawler extends ChileJumboCrawler {

  public ChileJumbovinaCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(ChileJumboCrawler.HOME_PAGE));
  }

  @Override
  public void handleCookiesBeforeFetch() {
    Logging.printLogDebug(logger, session, "Adding cookie...");

    BasicClientCookie cookie = new BasicClientCookie("VTEXSC", "sc=" + ChileJumboCrawler.JUMBO_VINA_ID);
    cookie.setDomain("." + ChileJumboCrawler.HOST);
    cookie.setPath("/");
    this.cookies.add(cookie);
  }
}
