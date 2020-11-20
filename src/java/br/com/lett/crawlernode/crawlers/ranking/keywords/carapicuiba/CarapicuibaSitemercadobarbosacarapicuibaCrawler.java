package br.com.lett.crawlernode.crawlers.ranking.keywords.carapicuiba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class CarapicuibaSitemercadobarbosacarapicuibaCrawler extends BrasilSitemercadoCrawler {

   public CarapicuibaSitemercadobarbosacarapicuibaCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/barbosa/carapicuiba-loja-37-vila-silva-vila-silva-ribeiro-av-inocencio-serafico";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
