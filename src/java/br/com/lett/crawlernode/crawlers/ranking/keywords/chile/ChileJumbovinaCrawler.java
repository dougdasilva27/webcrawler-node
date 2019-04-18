package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import org.apache.http.impl.cookie.BasicClientCookie;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.ChileJumboCrawler;
import br.com.lett.crawlernode.util.Logging;

public class ChileJumbovinaCrawler extends ChileJumboCrawler {

  public ChileJumbovinaCrawler(Session session) {
    super(session);
    super.storeCode = br.com.lett.crawlernode.crawlers.corecontent.extractionutils.ChileJumboCrawler.JUMBO_LAREINA_ID;
  }

  @Override
  protected void processBeforeFetch() {
    super.processBeforeFetch();

    Logging.printLogDebug(logger, session, "Adding cookie...");

    BasicClientCookie cookie = new BasicClientCookie("VTEXSC", "sc=" + storeCode);
    cookie.setDomain("." + ChileJumboCrawler.HOST);
    cookie.setPath("/");
    this.cookies.add(cookie);
  }
}
