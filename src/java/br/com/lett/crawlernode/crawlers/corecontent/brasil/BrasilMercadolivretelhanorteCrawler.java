package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.MercadolivreCrawler;

/**
 * Date: 06/01/2020
 * 
 * @author Marcos Moura
 *
 */

public class BrasilMercadolivretelhanorteCrawler extends MercadolivreCrawler {

   private static final String HOME_PAGE = "https://loja.mercadolivre.com.br/telhanorte";
   private static final String MAIN_SELLER_NAME_LOWER = "telhanorte";

   public BrasilMercadolivretelhanorteCrawler(Session session) {
      super(session);
      super.setHomePage(HOME_PAGE);
      super.setMainSellerNameLower(MAIN_SELLER_NAME_LOWER);
      super.setSeparator(',');
   }
}
