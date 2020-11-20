package br.com.lett.crawlernode.crawlers.corecontent.araras;

import java.util.HashMap;
import java.util.Map;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class ArarasSitemercadodeltasupermercadoCrawler extends BrasilSitemercadoCrawler {

   public ArarasSitemercadodeltasupermercadoCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/deltasupermercados/araras-loja-araras-jardim-santa-cruz-r-das-esmeraldas";

   public static final int IDLOJA = 4916;
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
