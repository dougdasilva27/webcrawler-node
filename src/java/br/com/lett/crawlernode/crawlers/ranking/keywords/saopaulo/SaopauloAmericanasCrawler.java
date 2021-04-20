package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.B2WScriptPageCrawlerRanking;

public class SaopauloAmericanasCrawler extends B2WScriptPageCrawlerRanking {

   private static final String HOME_PAGE = "https://www.americanas.com.br/";

   public SaopauloAmericanasCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
