package br.com.lett.crawlernode.crawlers.corecontent.florianopolis;


import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BistekCrawler;

public class FlorianopolisBistekCrawler extends BistekCrawler {

   private static final String LOCATION = "12";

   public FlorianopolisBistekCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocation() {
      return "loja" + LOCATION;
   }
}
