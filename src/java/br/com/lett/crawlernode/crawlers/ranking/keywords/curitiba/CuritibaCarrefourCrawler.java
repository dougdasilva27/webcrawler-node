package br.com.lett.crawlernode.crawlers.ranking.keywords.curitiba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.brasil.BrasilCarrefourCrawler;

/**
 * 18/04/2020
 * 
 * @author Fabrício
 *
 */
public class CuritibaCarrefourCrawler extends BrasilCarrefourCrawler {

   public CuritibaCarrefourCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = br.com.lett.crawlernode.crawlers.corecontent.curitiba.CuritibaCarrefourCrawler.HOME_PAGE;
   public static final String LOCATION = br.com.lett.crawlernode.crawlers.corecontent.curitiba.CuritibaCarrefourCrawler.LOCATION;

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getLocation() {
      return LOCATION;
   }
}
