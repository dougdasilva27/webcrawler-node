package br.com.lett.crawlernode.crawlers.ranking.keywords.tatui;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class TatuiSitemercadobarbosatatuiCrawler extends BrasilSitemercadoCrawler {

   public TatuiSitemercadobarbosatatuiCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/barbosa/tatui-loja-12-tatui-centro-avenida-doutor-salles-gomes";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
