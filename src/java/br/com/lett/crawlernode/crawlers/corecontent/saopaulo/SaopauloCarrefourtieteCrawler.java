package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CarrefourCrawler;

/**
 * 18/04/2020
 * 
 * @author Fabrício
 *
 */
public class SaopauloCarrefourtieteCrawler extends CarrefourCrawler {

   public SaopauloCarrefourtieteCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://mercado.carrefour.com.br/";
   public static final String LOCATION = "02063-000";

   @Override
   protected String getLocation() {
      return LOCATION;
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
