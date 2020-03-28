package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class SaopauloSitemercadobarbosaraposotavaresCrawler extends BrasilSitemercadoCrawler {

   public SaopauloSitemercadobarbosaraposotavaresCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/barbosa/sao-paulo-loja-24-raposo-tavares-jardim-boa-vista-zona-oeste-sp-270";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
