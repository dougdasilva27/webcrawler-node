package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler;

public class SaopauloSupermercadoprecitoCrawler extends BrasilSitemercadoCrawler {

   public SaopauloSupermercadoprecitoCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/supermercadoprecito/sao-paulo-loja-praca-nippon-jardim-japao-praca-nippon";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
