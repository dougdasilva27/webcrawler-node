package br.com.lett.crawlernode.crawlers.ranking.keywords.cotia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class CotiaSitemercadobarbosacotiaCrawler extends BrasilSitemercadoCrawler {

   public CotiaSitemercadobarbosacotiaCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/barbosa/cotia-loja-23-cotia-centro-avenida-antonio-matias-de-camargo";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
