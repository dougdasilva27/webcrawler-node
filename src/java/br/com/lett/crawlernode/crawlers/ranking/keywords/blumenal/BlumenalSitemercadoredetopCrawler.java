package br.com.lett.crawlernode.crawlers.ranking.keywords.blumenal;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler;

public class BlumenalSitemercadoredetopCrawler extends BrasilSitemercadoCrawler {
   public static final String HOME_PAGE = "https://www.sitemercado.com.br/redetop/blumenau-loja-escola-agricola-asilo-r-benjamin-constant";

   public BlumenalSitemercadoredetopCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
