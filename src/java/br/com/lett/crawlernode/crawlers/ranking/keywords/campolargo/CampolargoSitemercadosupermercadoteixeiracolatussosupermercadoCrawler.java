package br.com.lett.crawlernode.crawlers.ranking.keywords.campolargo;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilSitemercadoCrawler;
import br.com.lett.crawlernode.util.CommonMethods;

/**
 * @author gabriel date: 2019-09-24
 */
public class CampolargoSitemercadosupermercadoteixeiracolatussosupermercadoCrawler extends BrasilSitemercadoCrawler {

   public CampolargoSitemercadosupermercadoteixeiracolatussosupermercadoCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/supermercadocolatusso/campo-largo-loja-aguas-claras-jardim-sao-vicente-rua-alcebiades-afonso-guimaraes";

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
