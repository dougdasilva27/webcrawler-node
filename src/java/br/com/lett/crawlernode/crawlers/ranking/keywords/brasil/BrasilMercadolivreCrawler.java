package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MercadolivreCrawler;

public class BrasilMercadolivreCrawler extends MercadolivreCrawler {

   private final String URL = "https://lista.mercadolivre.com.br/" + this.keywordWithoutAccents.replace(" ", "-");

   public BrasilMercadolivreCrawler(Session session) {
      super(session);
      super.setUrl(URL);
      super.setProductUrlHost("produto.mercadolivre.com.br");
      super.setNextUrlHost("lista.mercadolivre.com.br");
   }
}
