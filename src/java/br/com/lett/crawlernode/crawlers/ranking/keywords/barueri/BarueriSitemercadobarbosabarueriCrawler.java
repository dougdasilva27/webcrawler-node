package br.com.lett.crawlernode.crawlers.ranking.keywords.barueri;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class BarueriSitemercadobarbosabarueriCrawler extends BrasilSitemercadoCrawler {

   public BarueriSitemercadobarbosabarueriCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/barbosa/barueri-loja-04-barueri-jardim-dos-camargos-rua-campos-sales";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
