package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.crawlers.extractionutils.ranking.VipcommerceRanking;
import br.com.lett.crawlernode.core.session.Session;

public class BrasilRedecomprasCrawler extends VipcommerceRanking {
   private static final String DOMAIN = "redecomprasdelivery.com.br";

   public BrasilRedecomprasCrawler(Session session) {
      super(session);
   }

   @Override
   public String getDomain() {
      return DOMAIN;
   }
}
