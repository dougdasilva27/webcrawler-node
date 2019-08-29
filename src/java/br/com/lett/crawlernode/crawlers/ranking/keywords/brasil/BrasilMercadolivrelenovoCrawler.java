package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.MercadolivreCrawler;

public class BrasilMercadolivrelenovoCrawler extends MercadolivreCrawler {

  private final String URL = "https://lista.mercadolivre.com.br/" + this.keywordWithoutAccents.replace(" ", "-") + "_Loja_lenovo#D[A:"
      + this.keywordWithoutAccents.replace(" ", "+") + ",lenovo]";

  public BrasilMercadolivrelenovoCrawler(Session session) {
    super(session);
    super.setUrl(URL);
    super.setProductUrlHost("produto.mercadolivre.com.br");
    super.setNextUrlHost("lista.mercadolivre.com.br");
  }
}
