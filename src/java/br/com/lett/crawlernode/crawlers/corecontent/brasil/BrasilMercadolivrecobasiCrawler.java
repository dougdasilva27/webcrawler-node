package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.MercadolivreCrawler;

/**
 * Date: 16/01/2020
 * 
 * @author Marcos Moura
 *
 */

public class BrasilMercadolivrecobasiCrawler extends MercadolivreCrawler {

   private static final String HOME_PAGE = "https://loja.mercadolivre.com.br/cobasi";
   private static final String MAIN_SELLER_NAME_LOWER = "cobasi";

   public BrasilMercadolivrecobasiCrawler(Session session) {
      super(session);
      super.setHomePage(HOME_PAGE);
      super.setMainSellerNameLower(MAIN_SELLER_NAME_LOWER);
   }
}
