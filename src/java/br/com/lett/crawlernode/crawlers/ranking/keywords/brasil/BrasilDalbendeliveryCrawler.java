package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.VipcommerceRanking;

public class BrasilDalbendeliveryCrawler extends VipcommerceRanking {

   private static final String DOMAIN = "superdalben.com.br";

   public BrasilDalbendeliveryCrawler(Session session) {
      super(session);
   }

   @Override
   public String getDomain() {
      return DOMAIN;
   }
}
