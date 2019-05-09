package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilMercadolivreCrawler;

/**
 * Date: 08/10/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilMercadolivremelittaCrawler extends BrasilMercadolivreCrawler {

  private static final String HOME_PAGE = "https://loja.mercadolivre.com.br/coty";
  private static final String MAIN_SELLER_NAME_LOWER = "coty";

  public BrasilMercadolivremelittaCrawler(Session session) {
    super(session);
    super.setHomePage(HOME_PAGE);
    super.setMainSellerNameLower(MAIN_SELLER_NAME_LOWER);
  }
}
