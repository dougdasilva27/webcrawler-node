package br.com.lett.crawlernode.crawlers.ranking.keywords.curitiba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2020-04-11
 */
public class CuritibaSitemercadocondornilopecanhaCrawler extends BrasilSitemercadoCrawler {

   public CuritibaSitemercadocondornilopecanhaCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/condor/curitiba-loja-hiper-condor-nilo-pecanha-ahu-r-nilo-pecanha";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
