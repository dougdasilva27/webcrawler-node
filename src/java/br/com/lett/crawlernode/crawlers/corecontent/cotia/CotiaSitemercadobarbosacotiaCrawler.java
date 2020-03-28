package br.com.lett.crawlernode.crawlers.corecontent.cotia;

import java.util.HashMap;
import java.util.Map;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class CotiaSitemercadobarbosacotiaCrawler extends BrasilSitemercadoCrawler {

   public CotiaSitemercadobarbosacotiaCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/barbosa/cotia-loja-23-cotia-centro-avenida-antonio-matias-de-camargo";

   public static final int IDLOJA = 556;
   public static final int IDREDE = 135;

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
