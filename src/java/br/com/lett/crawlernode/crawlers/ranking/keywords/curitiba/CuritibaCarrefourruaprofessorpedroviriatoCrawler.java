package br.com.lett.crawlernode.crawlers.ranking.keywords.curitiba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.CarrefourCrawler;

public class CuritibaCarrefourruaprofessorpedroviriatoCrawler extends CarrefourCrawler {
   private static final String HOME_PAGE = br.com.lett.crawlernode.crawlers.corecontent.curitiba.CuritibaCarrefourruaprofessorpedroviriatoCrawler.HOME_PAGE;
   public static final String LOCATION = br.com.lett.crawlernode.crawlers.corecontent.curitiba.CuritibaCarrefourruaprofessorpedroviriatoCrawler.LOCATION;

   public CuritibaCarrefourruaprofessorpedroviriatoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getLocation() {
      return LOCATION;
   }

}
