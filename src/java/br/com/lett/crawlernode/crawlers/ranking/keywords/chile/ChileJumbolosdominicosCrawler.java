package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import org.apache.http.impl.cookie.BasicClientCookie;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.ChileJumboCrawler;
import br.com.lett.crawlernode.util.Logging;

public class ChileJumbolosdominicosCrawler extends ChileJumboCrawler {

  public ChileJumbolosdominicosCrawler(Session session) {
    super(session);
    super.storeCode = br.com.lett.crawlernode.crawlers.extractionutils.core.ChileJumboCrawler.JUMBO_LOSDOMINICOS_ID;
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
