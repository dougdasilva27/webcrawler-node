package br.com.lett.crawlernode.crawlers.ranking.keywords.sorocaba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class SorocabaSitemercadobarbosasorocabaCrawler extends BrasilSitemercadoCrawler {

   public SorocabaSitemercadobarbosasorocabaCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/barbosa/sorocaba-loja-36-sorocaba-jardim-santa-rosalia-avenida-dom-aguirre";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
