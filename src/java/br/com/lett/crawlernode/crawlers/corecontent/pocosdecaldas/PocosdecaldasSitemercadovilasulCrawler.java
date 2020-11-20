package br.com.lett.crawlernode.crawlers.corecontent.pocosdecaldas;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class PocosdecaldasSitemercadovilasulCrawler extends BrasilSitemercadoCrawler {

   public PocosdecaldasSitemercadovilasulCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "http://www.vilasul.com.br/";

   public static final int IDLOJA = 751;
   public static final int IDREDE = 675;

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getLoadPayload() {
      JSONObject payload = new JSONObject();

      payload.put("lojaUrl", "pocos-de-caldas-shopping-pocos-de-caldas-vila-cascata-das-antas-avenida-silvio-monteiro-dos-santos");
      payload.put("redeUrl", "vilasul");

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
