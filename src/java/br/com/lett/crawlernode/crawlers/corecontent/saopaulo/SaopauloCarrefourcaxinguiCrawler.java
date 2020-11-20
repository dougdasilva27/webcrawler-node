package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CarrefourCrawler;

/**
 * 18/04/2020
 * 
 * @author Fabrício
 *
 */
public class SaopauloCarrefourcaxinguiCrawler extends CarrefourCrawler {

   public SaopauloCarrefourcaxinguiCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://mercado.carrefour.com.br/";
   public static final String LOCATION = "05516-030";

   @Override
   protected String getLocation() {
      return LOCATION;
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
