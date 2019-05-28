package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.MercadolivreCrawler;

public class MexicoMercadolibreheinzCrawler extends MercadolivreCrawler {

  public MexicoMercadolibreheinzCrawler(Session session) {
    super(session);
    super.setStoreName("heinz");
    super.setNextUrlHost("listado.mercadolibre.com.mx");
    super.setProductUrlHost("articulo.mercadolibre.com.mx");
  }

}
