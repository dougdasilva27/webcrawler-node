package br.com.lett.crawlernode.crawlers.ranking.keywords.charqueada;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class CharqueadaSitemercadodeltasupermercadoCrawler extends BrasilSitemercadoCrawler {

   public CharqueadaSitemercadodeltasupermercadoCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/deltasupermercados/charqueada-loja-charqueada-centro-r-governador-pedro-de-toledo";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

}
