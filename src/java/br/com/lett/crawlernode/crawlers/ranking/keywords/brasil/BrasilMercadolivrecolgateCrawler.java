package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MercadolivreCrawler;

public class BrasilMercadolivrecolgateCrawler extends MercadolivreCrawler {

   private final String URL = "https://lista.mercadolivre.com.br/" + this.keywordWithoutAccents.replace(" ", "-") + "_Loja_colgatepalmolive#D[A:"
         + this.keywordWithoutAccents.replace(" ", "+") + ",colgatepalmolive]";

   public BrasilMercadolivrecolgateCrawler(Session session) {
      super(session);
      super.setUrl(URL);
      super.setProductUrlHost("produto.mercadolivre.com.br");
      super.setNextUrlHost("lista.mercadolivre.com.br");
   }
}
