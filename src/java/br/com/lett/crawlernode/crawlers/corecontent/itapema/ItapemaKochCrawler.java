package br.com.lett.crawlernode.crawlers.corecontent.itapema;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.KochCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;

/**
 * Date: 09/07/2019
 *
 * @author Gabriel Dornelas
 */
public class ItapemaKochCrawler extends KochCrawler {

   protected static final String LOCATION_COOKIE_VALUE = "1";

   public ItapemaKochCrawler(Session session) {
      super(session);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      super.handleCookiesBeforeFetch();
      this.cookies.add(CrawlerUtils.setCookie(LOCATION_COOKIE_NAME, LOCATION_COOKIE_VALUE, LOCATION_COOKIE_DOMAIN, LOCATION_COOKIE_PATH));
   }

}
