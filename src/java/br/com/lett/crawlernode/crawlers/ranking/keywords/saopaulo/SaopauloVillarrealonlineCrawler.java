package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.VipcommerceRanking;

public class SaopauloVillarrealonlineCrawler extends VipcommerceRanking {
   private static final String DOMAIN = "villarrealonline.com.br";

   public SaopauloVillarrealonlineCrawler(Session session) {
      super(session);
   }

   @Override
   public String getDomain() {
      return DOMAIN;
   }
}
