package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.MercadolivreCrawler;

/**
 * Date: 28/08/2019
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilMercadolivreCrawler extends MercadolivreCrawler {

  private static final String HOME_PAGE = "https://www.mercadolivre.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "mercadolivre";

  public BrasilMercadolivreCrawler(Session session) {
    super(session);
    super.setHomePage(HOME_PAGE);
    super.setMainSellerNameLower(MAIN_SELLER_NAME_LOWER);
    super.setSeparator(',');
  }
}
