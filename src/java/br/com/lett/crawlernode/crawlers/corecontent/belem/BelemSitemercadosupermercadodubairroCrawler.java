package br.com.lett.crawlernode.crawlers.corecontent.belem;

import java.util.HashMap;
import java.util.Map;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class BelemSitemercadosupermercadodubairroCrawler extends BrasilSitemercadoCrawler {

   public BelemSitemercadosupermercadodubairroCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/supermercadodubairro/belem-loja-julio-cesar-val-de-caes-avenida-julio-cesar";

   public static final int IDLOJA = 2535;
   public static final int IDREDE = 1649;

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
