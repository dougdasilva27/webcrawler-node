package br.com.lett.crawlernode.crawlers.ranking.keywords.indaiatuba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2020-04-11
 */
public class IndaiatubaSitemercadosaovicenteCrawler extends BrasilSitemercadoCrawler {

   public IndaiatubaSitemercadosaovicenteCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/supermercadossaovicente/indaiatuba-loja-sao-vicente-kioto-jardim-kioto-i-av-francisco-de-paula-leite";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
