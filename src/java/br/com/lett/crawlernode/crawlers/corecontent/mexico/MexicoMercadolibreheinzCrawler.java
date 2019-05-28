package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.MercadolivreCrawler;

public class MexicoMercadolibreheinzCrawler extends MercadolivreCrawler {

  private static final String HOME_PAGE = "https://tienda.mercadolivre.com.br/heinz";
  private static final String MAIN_SELLER_NAME_LOWER = "heinz";

  public MexicoMercadolibreheinzCrawler(Session session) {
    super(session);
    super.setHomePage(HOME_PAGE);
    super.setMainSellerNameLower(MAIN_SELLER_NAME_LOWER);
  }



}
