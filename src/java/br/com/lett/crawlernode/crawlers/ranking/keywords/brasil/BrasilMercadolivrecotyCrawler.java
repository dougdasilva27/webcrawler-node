package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.MercadolivreCrawler;

public class BrasilMercadolivrecotyCrawler extends MercadolivreCrawler {

  private final String URL = "https://lista.mercadolivre.com.br/" + this.keywordWithoutAccents.replace(" ", "-") + "_Loja_coty#D[A:"
      + this.keywordWithoutAccents.replace(" ", "+") + ",O:coty]";

  public BrasilMercadolivrecotyCrawler(Session session) {
    super(session);
    super.setProductUrlHost("produto.mercadolivre.com.br");
    super.setNextUrlHost("lista.mercadolivre.com.br");
    super.setUrl(URL);
  }
}
