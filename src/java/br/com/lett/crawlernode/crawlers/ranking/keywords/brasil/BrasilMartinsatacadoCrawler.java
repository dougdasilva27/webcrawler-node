package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MartinsKeywords;

public class BrasilMartinsatacadoCrawler extends MartinsKeywords {

   public BrasilMartinsatacadoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getPassword() {
      return "Nestle2020";
   }

   @Override
   protected String getLogin() {
      return "thais.araujo1@br.nestle.com";
   }
}


