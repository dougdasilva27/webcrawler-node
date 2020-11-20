package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2020-04-11
 */
public class SaopauloSitemercadoemporiovillamarinCrawler extends BrasilSitemercadoCrawler {

   public SaopauloSitemercadoemporiovillamarinCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/emporiovillamarin/sao-paulo-emporio-villamarin-vila-regente-feijo-r-capituba";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
