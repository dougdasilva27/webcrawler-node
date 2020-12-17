package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.VipcommerceRanking;

public class BrasilRexdeliveryCrawler extends VipcommerceRanking {
   private static final String DOMAIN = "rexdelivery.com.br";

   public BrasilRexdeliveryCrawler(Session session) {
      super(session);
   }

   @Override
   public String getDomain() {
      return DOMAIN;
   }
}

