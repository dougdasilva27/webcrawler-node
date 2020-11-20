package br.com.lett.crawlernode.crawlers.ranking.keywords.saojose;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BistekCrawler;

public class SaojoseBistekCrawler extends BistekCrawler {

   private static final String LOCATION = "16";

   public SaojoseBistekCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocation() {
      return "loja" + LOCATION;
   }
}
