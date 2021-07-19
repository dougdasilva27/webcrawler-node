package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.brasil.BrasilCarrefourCrawler;

/**
 * Date: 19/10/20
 *
 * @author Fellype Layunne
 */
public class SaopauloCarrefourvilagertrudesCrawler extends BrasilCarrefourCrawler {

   public SaopauloCarrefourvilagertrudesCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = br.com.lett.crawlernode.crawlers.corecontent.saopaulo.SaopauloCarrefourvilagertrudesCrawler.HOME_PAGE;
   public static final String LOCATION = br.com.lett.crawlernode.crawlers.corecontent.saopaulo.SaopauloCarrefourvilagertrudesCrawler.LOCATION;
   public static final String LOCATION_TOKEN = br.com.lett.crawlernode.crawlers.corecontent.saopaulo.SaopauloCarrefourvilagertrudesCrawler.LOCATION_TOKEN;

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getCep() {
      return LOCATION;
   }

   @Override
   protected String getLocation() {
      return LOCATION_TOKEN;
   }
}
