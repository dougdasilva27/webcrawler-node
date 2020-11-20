package br.com.lett.crawlernode.crawlers.ranking.keywords.americana;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2020-04-11
 */
public class AmericanaSitemercadosaovicenteavcilloCrawler extends BrasilSitemercadoCrawler {

   public AmericanaSitemercadosaovicenteavcilloCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/supermercadossaovicente/americana-loja-sao-vicente-av-cillos-jardim-sao-jose-av-de-cillo";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
