package br.com.lett.crawlernode.crawlers.corecontent.novohorizonte;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilSitemercadoCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class NovohorizonteSitemercadobigmartCrawler extends BrasilSitemercadoCrawler {

   private static final String HOME_PAGE = "https://www.sitemercado.com.br/bigmartsupermercados/novo-horizonte-loja-novo-horizonte-centro-av-domingos-baraldo";

   public static final int IDLOJA = 4190;
   public static final int IDREDE = 2390;

   public NovohorizonteSitemercadobigmartCrawler(Session session) {
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

   @Override
   protected Map<String, Integer> getLojaInfo() {
      Map<String, Integer> lojaInfo = new HashMap<>();
      lojaInfo.put("IdLoja", IDLOJA);
      lojaInfo.put("IdRede", IDREDE);
      return lojaInfo;
   }
}
