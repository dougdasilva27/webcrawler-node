package br.com.lett.crawlernode.crawlers.ranking.keywords.maringa;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2020-04-11
 */
public class MaringaSitemercadocidadecancaoCrawler extends BrasilSitemercadoCrawler {

   public MaringaSitemercadocidadecancaoCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/supermercadoscidadecancao/maringa-loja-maringa-01-zona-05-avenida-brasil";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
