package br.com.lett.crawlernode.crawlers.ranking.keywords.portoalegre;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.VtexRankingKeywordsNew;

public class PortoalegreTreichelCrawler extends VtexRankingKeywordsNew {
   public PortoalegreTreichelCrawler(Session session) {
      super(session);
   }

   @Override
   protected String setHomePage() {
      return session.getOptions().optString("homePage");
   }
}
