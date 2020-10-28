package br.com.lett.crawlernode.crawlers.ranking.keywords.ourinhos;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilSitemercadoCrawler;

public class OurinhosSitemercadosaojudastadeuCrawler extends BrasilSitemercadoCrawler {

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/supermercadossaojudastadeu/ourinhos-loja-vila-musa-vila-santa-maria-av-domingos-camerlingo-calo";

   public OurinhosSitemercadosaojudastadeuCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
