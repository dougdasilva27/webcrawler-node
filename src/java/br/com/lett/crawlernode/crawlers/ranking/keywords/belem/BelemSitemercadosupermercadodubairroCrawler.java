package br.com.lett.crawlernode.crawlers.ranking.keywords.belem;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class BelemSitemercadosupermercadodubairroCrawler extends BrasilSitemercadoCrawler {

   public BelemSitemercadosupermercadodubairroCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/supermercadodubairro/belem-loja-julio-cesar-val-de-caes-avenida-julio-cesar";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
