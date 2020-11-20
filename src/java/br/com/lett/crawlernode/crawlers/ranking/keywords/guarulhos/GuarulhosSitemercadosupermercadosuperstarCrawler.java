package br.com.lett.crawlernode.crawlers.ranking.keywords.guarulhos;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler;

public class GuarulhosSitemercadosupermercadosuperstarCrawler extends BrasilSitemercadoCrawler {

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/mercadoss/guarulhos-loja-jardim-santa-mena-jardim-santa-mena-rua-condessa-amalia";

   public GuarulhosSitemercadosupermercadosuperstarCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
