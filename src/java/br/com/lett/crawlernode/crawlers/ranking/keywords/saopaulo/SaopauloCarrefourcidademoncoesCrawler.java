package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.brasil.BrasilCarrefourCrawler;

public class SaopauloCarrefourcidademoncoesCrawler extends BrasilCarrefourCrawler {

   private static final String HOME_PAGE = br.com.lett.crawlernode.crawlers.corecontent.saopaulo.SaopauloCarrefourcidademoncoesCrawler.HOME_PAGE;
   public static final String LOCATION = br.com.lett.crawlernode.crawlers.corecontent.saopaulo.SaopauloCarrefourcidademoncoesCrawler.LOCATION;
   public static final String LOCATION_TOKEN = br.com.lett.crawlernode.crawlers.corecontent.saopaulo.SaopauloCarrefourcidademoncoesCrawler.LOCATION_TOKEN;

   public SaopauloCarrefourcidademoncoesCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCep() {
      return LOCATION;
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getLocation() {
      return LOCATION_TOKEN;
   }
}
