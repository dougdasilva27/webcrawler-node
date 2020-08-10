package br.com.lett.crawlernode.crawlers.ranking.keywords.taubate;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.ComprebemCrawlerRanking;

public class TaubateComprebemCrawler extends ComprebemCrawlerRanking {

   private final String HOME_PAGE = "delivery.comprebem.com.br";
   private final String CEP = "12010-600";

   public TaubateComprebemCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getCep() {
      return CEP;
   }
}
