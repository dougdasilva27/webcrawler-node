package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.MercadolivreCrawler;

public class BrasilMercadolivrecotyCrawler extends MercadolivreCrawler {

  public BrasilMercadolivrecotyCrawler(Session session) {
    super(session);
    super.setStoreName("coty");
    super.setNextUrlHost("lista.mercadolivre.com.br");
    super.setProductUrlHost("produto.mercadolivre.com.br");
  }
}
