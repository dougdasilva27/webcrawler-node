package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilSitemercadoCrawler;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SaopauloSupermercadoprecitoCrawler extends BrasilSitemercadoCrawler {

   public SaopauloSupermercadoprecitoCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/supermercadoprecito/sao-paulo-loja-praca-nippon-jardim-japao-praca-nippon";

   public static final int IDLOJA = 983;
   public static final int IDREDE = 902;

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
