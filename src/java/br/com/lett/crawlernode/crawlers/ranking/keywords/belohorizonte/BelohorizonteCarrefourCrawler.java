package br.com.lett.crawlernode.crawlers.ranking.keywords.belohorizonte;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.CarrefourCrawler;

public class BelohorizonteCarrefourCrawler extends CarrefourCrawler {


   public BelohorizonteCarrefourCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "https://mercado.carrefour.com.br/";
   public static final String LOCATION = br.com.lett.crawlernode.crawlers.corecontent.belohorizonte.BelohorizonteCarrefourCrawler.LOCATION;

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getLocation() {
      return LOCATION;
   }
}
