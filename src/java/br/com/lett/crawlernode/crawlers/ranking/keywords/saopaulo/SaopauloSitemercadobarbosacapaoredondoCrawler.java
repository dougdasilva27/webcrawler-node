package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class SaopauloSitemercadobarbosacapaoredondoCrawler extends BrasilSitemercadoCrawler {

   public SaopauloSitemercadobarbosacapaoredondoCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/barbosa/sao-paulo-loja-35-capao-redondo-capao-redondo-avenida-comendador-sant-anna";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
