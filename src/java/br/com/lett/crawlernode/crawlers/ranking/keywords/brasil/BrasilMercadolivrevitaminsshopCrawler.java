package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.MercadolivreCrawler;

public class BrasilMercadolivrevitaminsshopCrawler extends MercadolivreCrawler {

  private static final String URL_STORE_NAME = "vitamins-shop";
  private static final String SEARCH_HOST = "lista.mercadolivre.com.br";
  private static final String PRODUCT_HOST = "produto.mercadolivre.com.br";
  private static final String STORE_TYPE = "vitamins-shop";

  public BrasilMercadolivrevitaminsshopCrawler(Session session) {
    super(session);
    super.setStoreName(URL_STORE_NAME);
    super.setNextUrlHost(SEARCH_HOST);
    super.setProductUrlHost(PRODUCT_HOST);
    super.setStoreType(STORE_TYPE);
  }
}
