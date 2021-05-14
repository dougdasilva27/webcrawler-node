package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.CarrefourCrawler;

public class SaopauloCarrefourbutantaavenidadeputadojacobCrawler extends CarrefourCrawler {

   private static final String HOME_PAGE = br.com.lett.crawlernode.crawlers.corecontent.saopaulo.SaopauloCarrefourbutantaavenidadeputadojacobCrawler.HOME_PAGE;
   public static final String CEP = br.com.lett.crawlernode.crawlers.corecontent.saopaulo.SaopauloCarrefourbutantaavenidadeputadojacobCrawler.CEP;

   public SaopauloCarrefourbutantaavenidadeputadojacobCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getLocation() {
      return br.com.lett.crawlernode.crawlers.corecontent.saopaulo.SaopauloCarrefourbutantaavenidadeputadojacobCrawler.LOCATION_TOKEN;
   }

   @Override
   protected String getCep() {
      return CEP;
   }
}
