package br.com.lett.crawlernode.crawlers.corecontent.hortolandia;

import java.util.HashMap;
import java.util.Map;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2020-11-04
 */
public class HortolandiaSitemercadogoddbomCrawler extends BrasilSitemercadoCrawler {
   public HortolandiaSitemercadogoddbomCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/goodbom/hortolandia-hortolandia-jardim-do-bosque-avenida-emancipacao/produtos/e-commerce/e-commerce";

   public static final int IDLOJA = 819;
   public static final int IDREDE = 287;

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