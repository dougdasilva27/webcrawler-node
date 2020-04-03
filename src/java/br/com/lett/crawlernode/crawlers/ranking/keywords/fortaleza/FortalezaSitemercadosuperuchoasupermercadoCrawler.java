package br.com.lett.crawlernode.crawlers.ranking.keywords.fortaleza;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class FortalezaSitemercadosuperuchoasupermercadoCrawler extends BrasilSitemercadoCrawler {

   public FortalezaSitemercadosuperuchoasupermercadoCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/superuchoasupermercado/fortaleza-loja-matriz-jardim-america-rua-andre-chaves";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
