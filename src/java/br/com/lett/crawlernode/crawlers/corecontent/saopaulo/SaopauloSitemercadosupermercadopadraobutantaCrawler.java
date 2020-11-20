package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class SaopauloSitemercadosupermercadopadraobutantaCrawler extends BrasilSitemercadoCrawler {

   public SaopauloSitemercadosupermercadopadraobutantaCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://supermercadovioleta.com.br/";

   public static final int IDLOJA = 4424;
   public static final int IDREDE = 2517;

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getLoadPayload() {
      JSONObject payload = new JSONObject();

      payload.put("lojaUrl", "sao-paulo-loja-butanta-butanta-av-vital-brasil");
      payload.put("redeUrl", "supermercadopadrao");

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
