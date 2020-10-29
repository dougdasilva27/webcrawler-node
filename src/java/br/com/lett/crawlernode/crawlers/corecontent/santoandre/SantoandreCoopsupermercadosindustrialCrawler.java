package br.com.lett.crawlernode.crawlers.corecontent.santoandre;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.CooprsupermercadosCrawler;


public class SantoandreCoopsupermercadosindustrialCrawler extends CooprsupermercadosCrawler {

   private static final String LOCATION = "U1cjY29vcHNwaWQ7";

   public SantoandreCoopsupermercadosindustrialCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocation() {
      return LOCATION;
   }
}
