package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class SaopauloSitemercadosupermercadopadraobutantaCrawler extends BrasilSitemercadoCrawler {

   public SaopauloSitemercadosupermercadopadraobutantaCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.supermercadopadrao.com.br/delivery/";

   @Override
   protected String getLoadPayload() {
      JSONObject payload = new JSONObject();

      payload.put("lojaUrl", "sao-paulo-loja-butanta-butanta-av-vital-brasil");
      payload.put("redeUrl", "supermercadopadrao");

      return payload.toString();
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
