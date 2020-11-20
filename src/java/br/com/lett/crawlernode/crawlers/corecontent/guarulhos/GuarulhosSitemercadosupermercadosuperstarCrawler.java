package br.com.lett.crawlernode.crawlers.corecontent.guarulhos;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilSitemercadoCrawler;
import java.util.HashMap;
import java.util.Map;

public class GuarulhosSitemercadosupermercadosuperstarCrawler extends BrasilSitemercadoCrawler {

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/mercadoss/guarulhos-loja-jardim-santa-mena-jardim-santa-mena-rua-condessa-amalia";

   public static final int IDLOJA = 1605;
   public static final int IDREDE = 1419;

   public GuarulhosSitemercadosupermercadosuperstarCrawler(Session session) {
      super(session);
   }

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
