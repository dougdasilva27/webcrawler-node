package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class SaopauloSitemercadovioletaedgarfacoCrawler extends BrasilSitemercadoCrawler {

   public SaopauloSitemercadovioletaedgarfacoCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://supermercadovioleta.com.br/";

   public static final int IDLOJA = 300;
   public static final int IDREDE = 157;

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getLoadPayload() {
      JSONObject payload = new JSONObject();

      payload.put("lojaUrl", "sao-paulo-loja-edgar-faco-vila-palmeiras-av-general-edgar-faco");
      payload.put("redeUrl", "violeta");

      return payload.toString();
   }

   @Override
   protected Map<String, Integer> getLojaInfo() {
      Map<String, Integer> lojaInfo = new HashMap<>();
      lojaInfo.put("IdLoja", IDLOJA);
      lojaInfo.put("IdRede", IDREDE);
      return lojaInfo;
   }
}
