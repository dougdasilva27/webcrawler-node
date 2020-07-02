package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilSitemercadoCrawler;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilSitemercadoidealCrawler extends BrasilSitemercadoCrawler {

   public BrasilSitemercadoidealCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/idealsupermercados/divinolandia-loja-divinolandia-centro-r-francisco-pereira-de-souza";

   public static final int IDLOJA = 3172;
   public static final int IDREDE = 429;

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
