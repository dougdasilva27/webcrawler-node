package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.MercadolivreCrawler;

public class MexicoMercadolibreheinzCrawler extends MercadolivreCrawler {

  private final String URL = "https://listado.mercadolibre.com.mx/" + this.keywordWithoutAccents.replace(" ", "-") + "_Tienda_heinz#D[A:"
      + this.keywordWithoutAccents.replace(" ", "+") + ",O:heinz]";

  public MexicoMercadolibreheinzCrawler(Session session) {
    super(session);
    super.setProductUrlHost("articulo.mercadolibre.com.mx");
    super.setNextUrlHost("listado.mercadolibre.com.mx");
    super.setUrl(URL);
  }

}
