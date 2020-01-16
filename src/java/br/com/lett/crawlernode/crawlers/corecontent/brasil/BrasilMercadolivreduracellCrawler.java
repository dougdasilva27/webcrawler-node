package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.MercadolivreCrawler;

/**
 * Date: 16/01/2020
 * 
 * @author Marcos Moura
 *
 */

public class BrasilMercadolivreduracellCrawler extends MercadolivreCrawler {

   private static final String HOME_PAGE = "https://loja.mercadolivre.com.br/duracell";
   private static final String MAIN_SELLER_NAME_LOWER = "duracell";

   public BrasilMercadolivreduracellCrawler(Session session) {
      super(session);
      super.setHomePage(HOME_PAGE);
      super.setMainSellerNameLower(MAIN_SELLER_NAME_LOWER);
      super.setSeparator(',');
   }
}
