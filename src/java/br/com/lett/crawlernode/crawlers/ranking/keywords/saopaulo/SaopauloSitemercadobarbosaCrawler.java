package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilSitemercadoCrawler;

public class SaopauloSitemercadobarbosaCrawler extends BrasilSitemercadoCrawler {
   public SaopauloSitemercadobarbosaCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE =
         "https://sitemercado.com.br/barbosa/sao-paulo-loja-10-pirituba-vila-pereira-barreto-av-benedito-de-andrade";
   public static final String LOAD_PAYLOAD =
         "{\"lojaUrl\":\"sao-paulo-loja-10-pirituba-vila-pereira-barreto-av-benedito-de-andrade\",\"redeUrl\":\"barbosa\"}";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getLoadPayload() {
      return LOAD_PAYLOAD;
   }
}
