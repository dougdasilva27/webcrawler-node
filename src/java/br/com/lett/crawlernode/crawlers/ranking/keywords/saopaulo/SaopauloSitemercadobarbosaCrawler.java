package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilSitemercadoCrawler;
import br.com.lett.crawlernode.util.CommonMethods;

public class SaopauloSitemercadobarbosaCrawler extends BrasilSitemercadoCrawler {
   public SaopauloSitemercadobarbosaCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://sitemercado.com.br/barbosa/sao-paulo-loja-10-pirituba-vila-pereira-barreto-av-benedito-de-andrade";

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
