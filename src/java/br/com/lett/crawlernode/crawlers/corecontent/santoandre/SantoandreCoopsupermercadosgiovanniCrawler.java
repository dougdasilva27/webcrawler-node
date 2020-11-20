package br.com.lett.crawlernode.crawlers.corecontent.santoandre;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CooprsupermercadosCrawler;

public class SantoandreCoopsupermercadosgiovanniCrawler extends CooprsupermercadosCrawler {

   private static final String LOCATION = "U1cjY29vcHNwZ3A7";

   public SantoandreCoopsupermercadosgiovanniCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocation() {
      return LOCATION;
   }
}
