package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.MercadolivreCrawler;

public class ChileMercadolibrepgCrawler extends MercadolivreCrawler {

   private static final String HOME_PAGE = "https://tienda.mercadolivre.cl/pg";
   private static final String MAIN_SELLER_NAME_LOWER = "P&G";

   public ChileMercadolibrepgCrawler(Session session) {
      super(session);
      super.setHomePage(HOME_PAGE);
      super.setMainSellerNameLower(MAIN_SELLER_NAME_LOWER);
   }

}
