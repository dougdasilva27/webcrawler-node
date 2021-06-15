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

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/supermercadossaovicente/indaiatuba-sao-vicente-indaiatuba-morada-do-sol-jardim-morada-do-sol-r-joao-martini";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
