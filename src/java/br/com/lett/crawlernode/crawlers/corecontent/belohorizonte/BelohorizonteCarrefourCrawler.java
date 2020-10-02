package br.com.lett.crawlernode.crawlers.corecontent.belohorizonte;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.CarrefourCrawler;

/**
 * 18/04/2020
 * 
 * @author Fabrício
 *
 */
public class BelohorizonteCarrefourCrawler extends CarrefourCrawler {

   public BelohorizonteCarrefourCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "https://mercado.carrefour.com.br/";
   public static final String LOCATION = "31310-250";

   @Override
   protected String getLocation() {
      return LOCATION;
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
