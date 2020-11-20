package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.MercadolivreCrawler;

/**
 * Date: 08/10/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilMercadolivrejvm2808Crawler extends MercadolivreCrawler {

   private static final String HOME_PAGE = "https://www.mercadolivre.com.br/perfil/JVM2808";
   private static final String MAIN_SELLER_NAME_LOWER = "JVM2808";

   public BrasilMercadolivrejvm2808Crawler(Session session) {
      super(session);
      super.setHomePage(HOME_PAGE);
      super.setMainSellerNameLower(MAIN_SELLER_NAME_LOWER);
   }
}