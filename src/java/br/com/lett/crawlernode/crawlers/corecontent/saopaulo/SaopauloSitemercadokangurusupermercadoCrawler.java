package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilSitemercadoCrawler;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SaopauloSitemercadokangurusupermercadoCrawler extends BrasilSitemercadoCrawler {

   public SaopauloSitemercadokangurusupermercadoCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/kangurusupermercado/sao-paulo-loja-tatuape-tatuape-r-antonio-de-barros";

   public static final int IDLOJA = 2723;
   public static final int IDREDE = 1485;

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getLoadPayload() {
      JSONObject payload = new JSONObject();

      payload.put("lojaUrl", "sao-paulo-loja-tatuape-tatuape-r-antonio-de-barros");
      payload.put("redeUrl", "kangurusupermercado");

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
