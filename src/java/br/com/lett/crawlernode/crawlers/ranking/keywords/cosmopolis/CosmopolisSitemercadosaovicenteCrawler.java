package br.com.lett.crawlernode.crawlers.ranking.keywords.cosmopolis;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2020-04-11
 */
public class CosmopolisSitemercadosaovicenteCrawler extends BrasilSitemercadoCrawler {

   public CosmopolisSitemercadosaovicenteCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/supermercadossaovicente/cosmopolis-loja-sao-vicente-cosmopolis-vila-nova-r-william-neumann";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
