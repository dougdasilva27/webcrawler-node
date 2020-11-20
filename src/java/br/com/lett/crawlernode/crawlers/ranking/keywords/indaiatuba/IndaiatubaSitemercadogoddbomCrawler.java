package br.com.lett.crawlernode.crawlers.ranking.keywords.indaiatuba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2020-04-11
 */
public class IndaiatubaSitemercadogoddbomCrawler extends BrasilSitemercadoCrawler {

   public IndaiatubaSitemercadogoddbomCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/goodbom/indaiatuba-loja-indaiatuba-jardim-hubert-rua-joao-giaquinto";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
