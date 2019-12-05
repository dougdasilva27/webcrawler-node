package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.MercadolivreCrawler;

public class ChileMercadolibrehuggiesCrawler extends MercadolivreCrawler {

   private static final String HOME_PAGE = "https://tienda.mercadolivre.cl/huggies";
   private static final String MAIN_SELLER_NAME_LOWER = "huggies";

   public ChileMercadolibrehuggiesCrawler(Session session) {
      super(session);
      super.setHomePage(HOME_PAGE);
      super.setMainSellerNameLower(MAIN_SELLER_NAME_LOWER);
      super.setSeparator(',');
   }

}
