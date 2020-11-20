package br.com.lett.crawlernode.crawlers.ranking.keywords.americana;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2020-04-11
 */
public class AmericanaSitemercadosaovicentesaovitoCrawler extends BrasilSitemercadoCrawler {

   public AmericanaSitemercadosaovicentesaovitoCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/supermercadossaovicente/americana-loja-sao-vicente-sao-vito-jardim-sao-vito-r-joao-bernestein";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
