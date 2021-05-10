package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.CarrefourCrawler;

public class BrasilCarrefourvilaregfeijoCrawler extends CarrefourCrawler {

   public BrasilCarrefourvilaregfeijoCrawler(Session session) {
      super(session);
   }


   public static final String HOME_PAGE = "https://mercado.carrefour.com.br/";
   public static final String LOCATION = "03342-900";


   @Override
   protected String getLocation() {
      return LOCATION;
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

}
