package br.com.lett.crawlernode.crawlers.ranking.keywords.campinas;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2020-04-11
 */
public class CampinasSitemercadogoddbomCrawler extends BrasilSitemercadoCrawler {

   public CampinasSitemercadogoddbomCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/goodbom/campinas-loja-sousas-jardim-conceicao-av-doutor-antonio-carlos-couto-de-barros";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
