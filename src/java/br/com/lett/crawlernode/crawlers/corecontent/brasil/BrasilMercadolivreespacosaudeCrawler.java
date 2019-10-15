package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.MercadolivreCrawler;

/**
 * Date: 15/10/2019
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilMercadolivreespacosaudeCrawler extends MercadolivreCrawler {

  private static final String HOME_PAGE = "https://loja.mercadolivre.com.br/espaco-saude";
  private static final String MAIN_SELLER_NAME_LOWER = "espaço saúde";

  public BrasilMercadolivreespacosaudeCrawler(Session session) {
    super(session);
    super.setHomePage(HOME_PAGE);
    super.setMainSellerNameLower(MAIN_SELLER_NAME_LOWER);
    super.setSeparator(',');
  }
}
