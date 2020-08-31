package br.com.lett.crawlernode.crawlers.ranking.keywords.piracicaba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class PiracicabaSitemercadodeltasupermercadoCrawler extends BrasilSitemercadoCrawler {

   public PiracicabaSitemercadodeltasupermercadoCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/deltasupermercados/piracicaba-loja-jardim-caxambu-jardim-caxambu-av-comendador-luciano-guidotti";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

}
