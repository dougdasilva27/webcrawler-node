package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CarrefourCrawler;

/**
 * 18/04/2020
 * 
 * @author Fabrício
 *
 */
public class SaopauloCarrefourmercadoCrawler extends CarrefourCrawler {


   public SaopauloCarrefourmercadoCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://mercado.carrefour.com.br/";

   @Override
   protected String getLocationToken() {
      return null;
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
