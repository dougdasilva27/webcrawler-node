package br.com.lett.crawlernode.crawlers.ranking.keywords.novaodessa;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2020-04-11
 */
public class NovaodessaSitemercadosaovicenteCrawler extends BrasilSitemercadoCrawler {

   public NovaodessaSitemercadosaovicenteCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/supermercadossaovicente/nova-odessa-loja-sao-vicente-ampelio-gazzetta-parque-industrial-harmonia-av-ampelio-gazzetta";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
