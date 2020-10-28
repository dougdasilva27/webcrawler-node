package br.com.lett.crawlernode.crawlers.corecontent.ourinhos;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilSitemercadoCrawler;

import java.util.HashMap;
import java.util.Map;

public class OurinhosSitemercadosaojudastadeuCrawler extends BrasilSitemercadoCrawler {

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/supermercadossaojudastadeu/ourinhos-loja-vila-musa-vila-santa-maria-av-domingos-camerlingo-calo";

   public static final int IDLOJA = 4726;
   public static final int IDREDE = 2705;

   public OurinhosSitemercadosaojudastadeuCrawler(Session session) {
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
