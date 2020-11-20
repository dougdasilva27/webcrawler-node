package br.com.lett.crawlernode.crawlers.ranking.keywords.tanabi;

import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler;
import br.com.lett.crawlernode.util.CommonMethods;

public class TanabiSitemercadosupermercadoproencaCrawler extends BrasilSitemercadoCrawler {

   public TanabiSitemercadosupermercadoproencaCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/proencasupermercados/tanabi-loja-tanabi-centro-av-ulisses-guimaraes";

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
