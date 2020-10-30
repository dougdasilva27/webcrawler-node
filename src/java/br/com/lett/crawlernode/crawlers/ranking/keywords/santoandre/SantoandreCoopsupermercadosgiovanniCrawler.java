package br.com.lett.crawlernode.crawlers.ranking.keywords.santoandre;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.CoopsupermercadosCrawler;

public class SantoandreCoopsupermercadosgiovanniCrawler extends CoopsupermercadosCrawler {

   private static final String LOCATION = "U1cjY29vcHNwZ3A7";

   public SantoandreCoopsupermercadosgiovanniCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocation() {
      return LOCATION;
   }
}
