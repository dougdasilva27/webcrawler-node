package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.VipcommerceRanking;

public class SaopauloSpanionlineCrawler extends VipcommerceRanking {
   private static final String DOMAIN = "spanionline.com.br";
   private static final String LOCATE_CODE = "10";

   public SaopauloSpanionlineCrawler(Session session) {
      super(session);
   }

   @Override
   public String getDomain() {
      return DOMAIN;
   }

   @Override
   public String getLocateCode() {
      return LOCATE_CODE;
   }
}
