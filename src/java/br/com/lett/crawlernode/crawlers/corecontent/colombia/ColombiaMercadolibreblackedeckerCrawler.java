package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.MercadolivreCrawler;

public class ColombiaMercadolibreblackedeckerCrawler extends MercadolivreCrawler {

   private static final String HOME_PAGE = "https://articulo.mercadolibre.com.co/";
   private static final String MAIN_SELLER_NAME_LOWER = "black and decker hogar";

   public ColombiaMercadolibreblackedeckerCrawler(Session session) {
      super(session);
      super.setHomePage(HOME_PAGE);
      super.setMainSellerNameLower(MAIN_SELLER_NAME_LOWER);
   }
}
