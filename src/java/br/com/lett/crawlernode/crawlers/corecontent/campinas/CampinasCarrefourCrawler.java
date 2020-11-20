package br.com.lett.crawlernode.crawlers.corecontent.campinas;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CarrefourCrawler;

/**
 * 18/04/2020
 * 
 * @author Fabrício
 *
 */
public class CampinasCarrefourCrawler extends CarrefourCrawler {

   public static final String HOME_PAGE = "https://mercado.carrefour.com.br/";
   public static final String LOCATION = "13094-693";

   public CampinasCarrefourCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocation() {
      return LOCATION;
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
