package br.com.lett.crawlernode.crawlers.ranking.keywords.divinolandia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class DivinolandiaSitemercadoidealCrawler extends BrasilSitemercadoCrawler {

   public DivinolandiaSitemercadoidealCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/idealsupermercados/divinolandia-loja-divinolandia-centro-r-francisco-pereira-de-souza";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
