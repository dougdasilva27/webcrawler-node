package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.MercadolivreCrawler;

/**
 * Date: 09/05/2019
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilMercadolivrecotyCrawler extends MercadolivreCrawler {

  private static final String HOME_PAGE = "https://loja.mercadolivre.com.br/coty";
  private static final String MAIN_SELLER_NAME_LOWER = "coty";

  public BrasilMercadolivrecotyCrawler(Session session) {
    super(session);
    super.setHomePage(HOME_PAGE);
    super.setMainSellerNameLower(MAIN_SELLER_NAME_LOWER);
  }
}
