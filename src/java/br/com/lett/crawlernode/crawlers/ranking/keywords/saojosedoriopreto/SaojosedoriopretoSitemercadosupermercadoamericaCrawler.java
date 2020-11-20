package br.com.lett.crawlernode.crawlers.ranking.keywords.saojosedoriopreto;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler;
import br.com.lett.crawlernode.util.CommonMethods;

/**
 * @author gabriel date: 2019-09-24
 */
public class SaojosedoriopretoSitemercadosupermercadoamericaCrawler extends BrasilSitemercadoCrawler {

   public SaojosedoriopretoSitemercadosupermercadoamericaCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/supermercadoamericarp/sao-jose-do-rio-preto-loja-jardim-bela-vista-jardim-bela-vista-avenida-doutor-antonio-tavares-pereira-lima";

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
