package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.CarrefourCrawler;

/**
 * 02/02/2018
 * 
 * @author gabriel
 *
 */
public class BrasilCarrefoursantoamaroCrawler extends CarrefourCrawler {


   public static final String HOME_PAGE = "https://mercado.carrefour.com.br/";
   public static final String LOCATION = "04754-030";


   public BrasilCarrefoursantoamaroCrawler(Session session) {
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
