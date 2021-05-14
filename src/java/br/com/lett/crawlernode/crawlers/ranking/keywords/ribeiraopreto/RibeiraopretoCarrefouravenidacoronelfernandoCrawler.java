package br.com.lett.crawlernode.crawlers.ranking.keywords.ribeiraopreto;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.CarrefourCrawler;

public class RibeiraopretoCarrefouravenidacoronelfernandoCrawler extends CarrefourCrawler {

   private static final String HOME_PAGE = br.com.lett.crawlernode.crawlers.corecontent.ribeiraopreto.RibeiraopretoCarrefouravenidacoronelfernandoCrawler.HOME_PAGE;

   public RibeiraopretoCarrefouravenidacoronelfernandoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getLocation() {
      return br.com.lett.crawlernode.crawlers.corecontent.ribeiraopreto.RibeiraopretoCarrefouravenidacoronelfernandoCrawler.LOCATION_TOKEN;
   }

   @Override
   protected String getCep() {
      return br.com.lett.crawlernode.crawlers.corecontent.ribeiraopreto.RibeiraopretoCarrefouravenidacoronelfernandoCrawler.CEP;
   }
}
