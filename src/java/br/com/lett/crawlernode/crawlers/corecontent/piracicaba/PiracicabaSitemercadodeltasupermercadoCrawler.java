package br.com.lett.crawlernode.crawlers.corecontent.piracicaba;

import java.util.HashMap;
import java.util.Map;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class PiracicabaSitemercadodeltasupermercadoCrawler extends BrasilSitemercadoCrawler {

   public PiracicabaSitemercadodeltasupermercadoCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/deltasupermercados/piracicaba-loja-jardim-caxambu-jardim-caxambu-av-comendador-luciano-guidotti";

   public static final int IDLOJA = 3023;
   public static final int IDREDE = 2268;

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected Map<String, Integer> getLojaInfo() {
      Map<String, Integer> lojaInfo = new HashMap<>();
      lojaInfo.put("IdLoja", IDLOJA);
      lojaInfo.put("IdRede", IDREDE);
      return lojaInfo;
   }
}
