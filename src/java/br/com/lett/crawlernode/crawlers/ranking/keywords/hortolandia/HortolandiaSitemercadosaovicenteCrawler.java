package br.com.lett.crawlernode.crawlers.ranking.keywords.hortolandia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2020-04-11
 */
public class HortolandiaSitemercadosaovicenteCrawler extends BrasilSitemercadoCrawler {

   public HortolandiaSitemercadosaovicenteCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/supermercadossaovicente/hortolandia-loja-sao-vicente-hortolandia-jardim-das-paineiras-r-orestes-denadai";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
