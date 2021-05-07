package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.B2WScriptPageCrawlerRanking;

public class SaopauloSubmarinoCrawler extends B2WScriptPageCrawlerRanking {

   private static final String HOME_PAGE = "https://www.submarino.com.br/";

   public SaopauloSubmarinoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }


}
