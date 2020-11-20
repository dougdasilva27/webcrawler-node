package br.com.lett.crawlernode.crawlers.ranking.keywords.hortolandia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2020-04-11
 */
public class HortolandiaSitemercadogoddbomCrawler extends BrasilSitemercadoCrawler {

   public HortolandiaSitemercadogoddbomCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/goodbom/hortolandia-hortolandia-jardim-do-bosque-avenida-emancipacao/produtos/e-commerce/e-commerce";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
