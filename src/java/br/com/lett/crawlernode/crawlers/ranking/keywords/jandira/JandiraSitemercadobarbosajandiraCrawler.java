package br.com.lett.crawlernode.crawlers.ranking.keywords.jandira;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class JandiraSitemercadobarbosajandiraCrawler extends BrasilSitemercadoCrawler {

   public JandiraSitemercadobarbosajandiraCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/barbosa/jandira-loja-08-jandira-centro-avenida-conceicao-sammartin";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
