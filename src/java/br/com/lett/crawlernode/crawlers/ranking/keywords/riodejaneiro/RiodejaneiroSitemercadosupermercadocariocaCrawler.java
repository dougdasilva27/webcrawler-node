package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler;
import br.com.lett.crawlernode.util.CommonMethods;

/**
 * @author gabriel date: 2019-09-24
 */
public class RiodejaneiroSitemercadosupermercadocariocaCrawler extends BrasilSitemercadoCrawler {

   public RiodejaneiroSitemercadosupermercadocariocaCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/supermercadocarioca/rio-de-janeiro-loja-paciencia-paciencia-rua-paulo-ribeiro-bastos";

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
