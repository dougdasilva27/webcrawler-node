package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.MercadolivreCrawler;

/**
 * Date: 13/10/2019
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilMercadolivrealbatrozmoveisCrawler extends MercadolivreCrawler {

  private static final String HOME_PAGE = "https://loja.mercadolivre.com.br/albatroz-moveis";
  private static final String MAIN_SELLER_NAME_LOWER = "albatroz móveis";

  public BrasilMercadolivrealbatrozmoveisCrawler(Session session) {
    super(session);
    super.setHomePage(HOME_PAGE);
    super.setMainSellerNameLower(MAIN_SELLER_NAME_LOWER);
    super.setSeparator(',');
  }
}
