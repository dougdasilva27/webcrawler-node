package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.MercadolivreCrawler;

public class BrasilMercadolivrevitaminsshopCrawler extends MercadolivreCrawler {

  public BrasilMercadolivrevitaminsshopCrawler(Session session) {
    super(session);
    super.setStoreName("vitamins-shop");
    super.setNextUrlHost("lista.mercadolivre.com.br");
    super.setProductUrlHost("produto.mercadolivre.com.br");
    super.setStoreType("Loja");
  }
}
