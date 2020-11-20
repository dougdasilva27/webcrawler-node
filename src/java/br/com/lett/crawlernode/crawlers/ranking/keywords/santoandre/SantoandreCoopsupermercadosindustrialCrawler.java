package br.com.lett.crawlernode.crawlers.ranking.keywords.santoandre;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.CoopsupermercadosCrawler;

public class SantoandreCoopsupermercadosindustrialCrawler extends CoopsupermercadosCrawler {

   private static final String LOCATION = "U1cjY29vcHNwaWQ7";

   public SantoandreCoopsupermercadosindustrialCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocation() {
      return LOCATION;
   }
}
