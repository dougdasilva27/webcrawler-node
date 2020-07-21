package br.com.lett.crawlernode.crawlers.ranking.keywords.cotia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class CotiaSitemercadoserranoCrawler extends BrasilSitemercadoCrawler {

   public CotiaSitemercadoserranoCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/serranosupermercados/cotia-loja-granja-viana-vila-santo-antonio-r-jose-felix-de-oliveira";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
