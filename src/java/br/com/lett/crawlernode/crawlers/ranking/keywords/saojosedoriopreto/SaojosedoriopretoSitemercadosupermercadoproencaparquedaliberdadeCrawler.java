package br.com.lett.crawlernode.crawlers.ranking.keywords.saojosedoriopreto;

import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilSitemercadoCrawler;
import br.com.lett.crawlernode.util.CommonMethods;

public class SaojosedoriopretoSitemercadosupermercadoproencaparquedaliberdadeCrawler extends BrasilSitemercadoCrawler {

   public SaojosedoriopretoSitemercadosupermercadoproencaparquedaliberdadeCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/proencasupermercados/sao-jose-do-rio-preto-loja-parque-da-liberdade-condominio-residencial-parque-da-liberdade-iv-av-jose-da-silva-se";


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


}
