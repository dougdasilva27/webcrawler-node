package br.com.lett.crawlernode.crawlers.corecontent.divinopolis;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.AbcsupermercadosCrawler;


public class DivinopolisAbcsupermercadosCrawler extends AbcsupermercadosCrawler {

   private static final String LOCATION = "1";

   public DivinopolisAbcsupermercadosCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocation() {
      return LOCATION;
   }
}
