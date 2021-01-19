package br.com.lett.crawlernode.crawlers.ranking.keywords.belohorizonte;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.VipcommerceRanking;

public class BelohorizonteVerdemarburitisCrawler extends VipcommerceRanking {

   private static final String DOMAIN = "verdemaratevoce.com.br";
   private static final String LOCATE_CODE = "2";

   public BelohorizonteVerdemarburitisCrawler(Session session) {
      super(session);
   }


   @Override
   protected String getLocateCode() {
      return LOCATE_CODE;
   }

   @Override
   protected String getDomain() {
      return DOMAIN;
   }
}
