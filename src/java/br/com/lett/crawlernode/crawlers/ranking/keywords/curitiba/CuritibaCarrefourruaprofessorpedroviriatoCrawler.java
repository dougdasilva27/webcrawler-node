package br.com.lett.crawlernode.crawlers.ranking.keywords.curitiba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.CarrefourCrawler;

public class CuritibaCarrefourruaprofessorpedroviriatoCrawler extends CarrefourCrawler {
   private static final String HOME_PAGE = br.com.lett.crawlernode.crawlers.corecontent.curitiba.CuritibaCarrefourruaprofessorpedroviriatoCrawler.HOME_PAGE;

   public CuritibaCarrefourruaprofessorpedroviriatoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getLocation() {
      return br.com.lett.crawlernode.crawlers.corecontent.curitiba.CuritibaCarrefourruaprofessorpedroviriatoCrawler.LOCATION_TOKEN;
   }

   @Override
   protected String getCep() {
      return br.com.lett.crawlernode.crawlers.corecontent.curitiba.CuritibaCarrefourruaprofessorpedroviriatoCrawler.CEP;
   }
}
