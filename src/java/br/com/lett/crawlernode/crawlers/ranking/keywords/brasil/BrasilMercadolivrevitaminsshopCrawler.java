package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.MercadolivreCrawler;

public class BrasilMercadolivrevitaminsshopCrawler extends MercadolivreCrawler {

  private final String URL = "https://lista.mercadolivre.com.br/" + this.keywordEncoded + "_Loja_vitamins-shop_af_to#origin=os_carousel";

  public BrasilMercadolivrevitaminsshopCrawler(Session session) {
    super(session);
    super.setUrl(URL);
    super.setProductUrlHost("produto.mercadolivre.com.br");
    super.setNextUrlHost("listado.mercadolibre.com.mx");
  }
}
