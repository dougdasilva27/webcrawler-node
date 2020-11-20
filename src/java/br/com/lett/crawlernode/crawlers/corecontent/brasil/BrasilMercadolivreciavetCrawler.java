package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.MercadolivreCrawler;

/**
 * Date: 08/10/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilMercadolivreciavetCrawler extends MercadolivreCrawler {

   private static final String HOME_PAGE = "https://www.mercadolivre.com.br/perfil/CIA%20VET";
   private static final String MAIN_SELLER_NAME_LOWER = "CIA VET";

   public BrasilMercadolivreciavetCrawler(Session session) {
      super(session);
      super.setHomePage(HOME_PAGE);
      super.setMainSellerNameLower(MAIN_SELLER_NAME_LOWER);
   }
}