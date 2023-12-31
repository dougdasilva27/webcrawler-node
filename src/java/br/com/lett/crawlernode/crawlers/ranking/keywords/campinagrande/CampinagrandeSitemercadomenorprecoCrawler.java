package br.com.lett.crawlernode.crawlers.ranking.keywords.campinagrande;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import org.json.JSONObject;

/**
 * @author gabriel date: 2019-09-24
 */
public class CampinagrandeSitemercadomenorprecoCrawler extends BrasilSitemercadoCrawler {

   public CampinagrandeSitemercadomenorprecoCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/menorpreco/campina-grande-loja-matriz-cruzeiro-rua-almirante-barroso";

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
