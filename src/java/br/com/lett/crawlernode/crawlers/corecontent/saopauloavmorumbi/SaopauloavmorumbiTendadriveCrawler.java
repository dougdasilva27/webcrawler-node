package br.com.lett.crawlernode.crawlers.corecontent.saopauloavmorumbi;

import org.apache.http.impl.cookie.BasicClientCookie;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.saopaulo.SaopauloTendadriveCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

/**
 * Date: 04/09/17
 * 
 * @author gabriel
 *
 */

public class SaopauloavmorumbiTendadriveCrawler extends SaopauloTendadriveCrawler {

   private static final String HOME_PAGE = "http://www.tendaatacado.com.br/";

   public SaopauloavmorumbiTendadriveCrawler(Session session) {
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

      this.cookies.addAll(CrawlerUtils.fetchCookiesFromAPage(HOME_PAGE, null,
            ".www.tendaatacado.com.br", "/", cookies, session, dataFetcher));

      // shop id (AV guarapiranga)
      BasicClientCookie cookie = new BasicClientCookie("VTEXSC", "sc=10");
      cookie.setDomain(".www.tendaatacado.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
   }
}
