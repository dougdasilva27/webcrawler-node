package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.CarrefourCrawler;

public class BrasilCarrefourCrawler extends CarrefourCrawler {

   private static final String HOME_PAGE = "https://www.carrefour.com.br/";

   public BrasilCarrefourCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getLocation() {
      return "";
   }
}
