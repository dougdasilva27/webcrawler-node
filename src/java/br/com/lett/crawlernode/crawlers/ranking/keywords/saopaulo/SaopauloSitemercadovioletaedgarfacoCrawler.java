package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class SaopauloSitemercadovioletaedgarfacoCrawler extends BrasilSitemercadoCrawler {

   public SaopauloSitemercadovioletaedgarfacoCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://supermercadovioleta.com.br/";

   @Override
   protected String getLoadPayload() {
      JSONObject payload = new JSONObject();

      payload.put("lojaUrl", "sao-paulo-loja-edgar-faco-vila-palmeiras-av-general-edgar-faco");
      payload.put("redeUrl", "violeta");

      return payload.toString();
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
