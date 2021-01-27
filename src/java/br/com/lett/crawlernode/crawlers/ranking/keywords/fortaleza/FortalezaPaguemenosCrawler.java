package br.com.lett.crawlernode.crawlers.ranking.keywords.fortaleza;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.VTEXRankingKeywords;

public class FortalezaPaguemenosCrawler extends VTEXRankingKeywords {

   private static final String HOME_PAGE = "https://www.paguemenos.com.br/";

   public FortalezaPaguemenosCrawler(Session session) {
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
