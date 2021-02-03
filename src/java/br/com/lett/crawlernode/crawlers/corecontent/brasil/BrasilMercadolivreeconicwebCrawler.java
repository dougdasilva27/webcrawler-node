package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.MercadolivreCrawler;

/**
 * Date: 08/10/2018
 *
 * @author Gabriel Dornelas
 */
public class BrasilMercadolivreeconicwebCrawler extends MercadolivreCrawler {

   private static final String HOME_PAGE = "https://eshops.mercadolivre.com.br/e-conic%20web";
   private static final String MAIN_SELLER_NAME_LOWER = "E-CONIC WEB";

   public BrasilMercadolivreeconicwebCrawler(Session session) {
      super(session);
      super.setHomePage(HOME_PAGE);
      super.setMainSellerNameLower(MAIN_SELLER_NAME_LOWER);
   }
}
