package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.MercadolivreCrawler;

/**
 * Date: 16/01/2020
 * 
 * @author Marcos Moura
 *
 */

public class BrasilMercadolivrepanasonicCrawler extends MercadolivreCrawler {

   private static final String HOME_PAGE = "https://loja.mercadolivre.com.br/panasonic";
   private static final String MAIN_SELLER_NAME_LOWER = "panasonic";

   public BrasilMercadolivrepanasonicCrawler(Session session) {
      super(session);
      super.setHomePage(HOME_PAGE);
      super.setMainSellerNameLower(MAIN_SELLER_NAME_LOWER);
      super.setSeparator(',');
   }

}
