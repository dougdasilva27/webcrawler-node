package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.VipcommerceRanking;

public class SaopauloSpanionlineCrawler extends VipcommerceRanking {
   private static final String DOMAIN = "spanionline.com.br";

   public SaopauloSpanionlineCrawler(Session session) {
      super(session);
   }

   @Override
   public String getDomain() {
      return DOMAIN;
   }
}
