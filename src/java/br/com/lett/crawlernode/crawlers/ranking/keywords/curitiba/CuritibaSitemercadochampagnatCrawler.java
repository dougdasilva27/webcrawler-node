package br.com.lett.crawlernode.crawlers.ranking.keywords.curitiba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2020-04-11
 */
public class CuritibaSitemercadochampagnatCrawler extends BrasilSitemercadoCrawler {

   public CuritibaSitemercadochampagnatCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/condor/curitiba-loja-champagnat-bigorrilho-r-martin-afonso";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
