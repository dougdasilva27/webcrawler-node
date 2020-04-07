package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class SaopauloSitemercadobarbosaermelinomatarazzoCrawler extends BrasilSitemercadoCrawler {

   public SaopauloSitemercadobarbosaermelinomatarazzoCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/barbosa/sao-paulo-loja-07-ermelino-matarazzo-vila-paranagua-rua-victoria-simionato";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
