package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.MercadolivreCrawler;

/**
 * Date: 08/10/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilMercadolivrelenovoCrawler extends MercadolivreCrawler {

  private static final String HOME_PAGE = "https://loja.mercadolivre.com.br/lenovo";
  private static final String MAIN_SELLER_NAME_LOWER = "lenovo";

  public BrasilMercadolivrelenovoCrawler(Session session) {
    super(session);
    super.setHomePage(HOME_PAGE);
    super.setMainSellerNameLower(MAIN_SELLER_NAME_LOWER);
    super.setSeparator(',');
  }
}
