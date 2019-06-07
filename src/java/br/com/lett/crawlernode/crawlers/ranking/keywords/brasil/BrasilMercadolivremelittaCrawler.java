package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.MercadolivreCrawler;

public class BrasilMercadolivremelittaCrawler extends MercadolivreCrawler {

  public BrasilMercadolivremelittaCrawler(Session session) {
    super(session);
    super.setStoreName("melitta");
    super.setNextUrlHost("lista.mercadolivre.com.br");
    super.setProductUrlHost("produto.mercadolivre.com.br");
    super.setStoreType("Loja");
  }
}
