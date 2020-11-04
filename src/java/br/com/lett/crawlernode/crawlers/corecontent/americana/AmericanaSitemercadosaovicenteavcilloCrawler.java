package br.com.lett.crawlernode.crawlers.corecontent.americana;

import java.util.HashMap;
import java.util.Map;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2020-11-04
 */
public class AmericanaSitemercadosaovicenteavcilloCrawler extends BrasilSitemercadoCrawler {
   public AmericanaSitemercadosaovicenteavcilloCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/supermercadossaovicente/americana-loja-sao-vicente-av-cillos-jardim-sao-jose-av-de-cillo";

   public static final int IDLOJA = 5818;
   public static final int IDREDE = 2663;

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