package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MartinsKeywords;

public class BrasilMartinsrbhealthCrawler extends MartinsKeywords {

   public BrasilMartinsrbhealthCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getPassword() {
      return "Ju271200";
   }

   @Override
   protected String getLogin() {
      return "heleno.Junior@rb.com";
   }
}
