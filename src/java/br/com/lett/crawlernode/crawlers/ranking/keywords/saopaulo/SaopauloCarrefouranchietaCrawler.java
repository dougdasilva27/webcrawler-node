package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.brasil.BrasilCarrefourCrawler;

/**
 * 18/04/2020
 * 
 * @author Fabrício
 *
 */
public class SaopauloCarrefouranchietaCrawler extends BrasilCarrefourCrawler {

   public SaopauloCarrefouranchietaCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = br.com.lett.crawlernode.crawlers.corecontent.saopaulo.SaopauloCarrefouranchietaCrawler.HOME_PAGE;
   public static final String LOCATION = br.com.lett.crawlernode.crawlers.corecontent.saopaulo.SaopauloCarrefouranchietaCrawler.LOCATION;
   public static final String LOCATION_TOKEN = br.com.lett.crawlernode.crawlers.corecontent.saopaulo.SaopauloCarrefouranchietaCrawler.LOCATION_TOKEN;

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
