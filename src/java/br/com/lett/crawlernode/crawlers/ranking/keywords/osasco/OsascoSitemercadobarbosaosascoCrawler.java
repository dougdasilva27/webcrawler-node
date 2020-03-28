package br.com.lett.crawlernode.crawlers.ranking.keywords.osasco;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class OsascoSitemercadobarbosaosascoCrawler extends BrasilSitemercadoCrawler {

   public OsascoSitemercadobarbosaosascoCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/barbosa/osasco-loja-33-osasco-conceicao-rua-pernambucana";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
