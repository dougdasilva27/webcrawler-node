package br.com.lett.crawlernode.crawlers.ranking.keywords.cerquilho;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class CerquilhoSitemercadodeltasupermercadoCrawler extends BrasilSitemercadoCrawler {

   public CerquilhoSitemercadodeltasupermercadoCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/deltasupermercados/cerquilho-loja-cerquilho-centro-avenida-dr-vinicio-gagliardi";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

}
