package br.com.lett.crawlernode.crawlers.corecontent.atibaia;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilSitemercadoCrawler;
import br.com.lett.crawlernode.util.CommonMethods;

/**
 * @author gabriel date: 2019-09-24
 */
public class AtibaiaSitemercadobigsupermercadosCrawler extends BrasilSitemercadoCrawler {

   public AtibaiaSitemercadobigsupermercadosCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/bigsupermercados/atibaia-loja-jardim-brasil-jardim-brasil-avenida-alfredo-andre";

   public static final int IDLOJA = 715;
   public static final int IDREDE = 643;

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

   @Override
   protected Map<String, Integer> getLojaInfo() {
      Map<String, Integer> lojaInfo = new HashMap<>();
      lojaInfo.put("IdLoja", IDLOJA);
      lojaInfo.put("IdRede", IDREDE);
      return lojaInfo;
   }
}
