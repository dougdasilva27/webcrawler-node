package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.VtexRankingKeywordsNew;

public class ColombiaOlimpicaCrawler extends VtexRankingKeywordsNew {

   @Override
   protected String setHomePage() {
      return "https://www.olimpica.com";
   }

   public ColombiaOlimpicaCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }
}
