package br.com.lett.crawlernode.crawlers.corecontent.saojosedoriopreto;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilSitemercadoCrawler;
import br.com.lett.crawlernode.util.CommonMethods;

public class SaojosedoriopretoparquedaliberdadeSitemercadosupermercadoproencaCrawler extends BrasilSitemercadoCrawler {

   public SaojosedoriopretoparquedaliberdadeSitemercadosupermercadoproencaCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/proencasupermercados/sao-jose-do-rio-preto-loja-parque-da-liberdade-condominio-residencial-parque-da-liberdade-iv-av-jose-da-silva-se";

   public static final int IDLOJA = 4636;
   public static final int IDREDE = 2137;

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getLoadPayload() {
      JSONObject payload = new JSONObject();
      String[] split = HOME_PAGE.split("/");

      payload.put("lojaUrl", CommonMethods.getLast(split));
      payload.put("redeUrl", split[split.length - 2]);

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
