package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilSitemercadoCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BrasilSitemercadocondorCrawler extends BrasilSitemercadoCrawler {

   public BrasilSitemercadocondorCrawler(Session session) {
   super(session);
}

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/condor/curitiba-loja-hiper-condor-novo-mundo-novo-mundo-r-visconde-do-serro-frio";

   public static final int IDLOJA = 4557;
   public static final int IDREDE = 2553;

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
