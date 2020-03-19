package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.HashMap;
import java.util.Map;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class SaopauloSitemercadobarbosaCrawler extends BrasilSitemercadoCrawler {

   public SaopauloSitemercadobarbosaCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE =
         "https://sitemercado.com.br/barbosa/sao-paulo-loja-10-pirituba-vila-pereira-barreto-av-benedito-de-andrade";
   public static final String LOAD_PAYLOAD =
         "{\"lojaUrl\":\"sao-paulo-loja-10-pirituba-vila-pereira-barreto-av-benedito-de-andrade\",\"redeUrl\":\"barbosa\"}";

   public static final int IDLOJA = 265;
   public static final int IDREDE = 135;

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getLoadPayload() {
      return LOAD_PAYLOAD;
   }

   @Override
   protected Map<String, Integer> getLojaInfo() {
      Map<String, Integer> lojaInfo = new HashMap<>();
      lojaInfo.put("IdLoja", IDLOJA);
      lojaInfo.put("IdRede", IDREDE);
      return lojaInfo;
   }
}
