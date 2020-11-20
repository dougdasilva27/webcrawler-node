package br.com.lett.crawlernode.crawlers.ranking.keywords.guararapes;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import org.json.JSONObject;

public class GuararapesSitemercadobigmartCrawler extends BrasilSitemercadoCrawler {

   private static final String HOME_PAGE = "https://www.sitemercado.com.br/bigmartsupermercados/guararapes-loja-guararapes-centro-al-baguassu";

   public GuararapesSitemercadobigmartCrawler(Session session) {
      super(session);
   }

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
