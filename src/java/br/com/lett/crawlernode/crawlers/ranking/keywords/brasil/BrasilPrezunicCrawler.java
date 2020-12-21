package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.VipcommerceRanking;

public class BrasilPrezunicCrawler extends VipcommerceRanking {
   private static final String DOMAIN = "delivery.prezunic.com.br";

   public BrasilPrezunicCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getDomain() {
      return DOMAIN;
   }
}
