package br.com.lett.crawlernode.crawlers.ranking.keywords.belohorizonte;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.CarrefourCrawler;

public class BelohorizonteCarrefourbelohorizontebelvedereCrawler extends CarrefourCrawler {

   private static final String HOME_PAGE = br.com.lett.crawlernode.crawlers.corecontent.belohorizonte.BelohorizonteCarrefourbelohorizontebelvedereCrawler.HOME_PAGE;
   public static final String LOCATION = br.com.lett.crawlernode.crawlers.corecontent.belohorizonte.BelohorizonteCarrefourbelohorizontebelvedereCrawler.LOCATION;
   public static final String LOCATION_TOKEN = br.com.lett.crawlernode.crawlers.corecontent.belohorizonte.BelohorizonteCarrefourbelohorizontebelvedereCrawler.LOCATION_TOKEN;

   public BelohorizonteCarrefourbelohorizontebelvedereCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getCep() {
      return LOCATION;
   }

   @Override
   protected String getLocation() {
      return LOCATION_TOKEN;
   }
}
