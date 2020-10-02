package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;

public class BrasilCarrefoursantoamaroCrawler extends BrasilCarrefourCrawler {

   public BrasilCarrefoursantoamaroCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = br.com.lett.crawlernode.crawlers.corecontent.brasil.BrasilCarrefoursantoamaroCrawler.HOME_PAGE;
   public static final String LOCATION = br.com.lett.crawlernode.crawlers.corecontent.brasil.BrasilCarrefoursantoamaroCrawler.LOCATION;

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getLocation() {
      return LOCATION;
   }
}
