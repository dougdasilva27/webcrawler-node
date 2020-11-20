package br.com.lett.crawlernode.crawlers.corecontent.divinolandia;

import java.util.HashMap;
import java.util.Map;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class DivinolandiaSitemercadoidealCrawler extends BrasilSitemercadoCrawler {

   public DivinolandiaSitemercadoidealCrawler(Session session) {
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
   protected Map<String, Integer> getLojaInfo() {
      Map<String, Integer> lojaInfo = new HashMap<>();
      lojaInfo.put("IdLoja", IDLOJA);
      lojaInfo.put("IdRede", IDREDE);
      return lojaInfo;
   }
}
