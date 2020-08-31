package br.com.lett.crawlernode.crawlers.ranking.keywords.araras;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class ArarasSitemercadodeltasupermercadoCrawler extends BrasilSitemercadoCrawler {

   public ArarasSitemercadodeltasupermercadoCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/deltasupermercados/araras-loja-araras-jardim-santa-cruz-r-das-esmeraldas";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

}
