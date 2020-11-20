package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MercadolivreCrawler;

public class BrasilMercadolivresoftysCrawler extends MercadolivreCrawler {

   private final String url = "https://lista.mercadolivre.com.br/" + this.keywordWithoutAccents.replace(" ", "-") + "_Loja_softys#D[A:"
         + this.keywordWithoutAccents.replace(" ", "+") + ",O:softys]";

   public BrasilMercadolivresoftysCrawler(Session session) {
      super(session);
      super.setProductUrlHost("produto.mercadolivre.com.br");
      super.setNextUrlHost("lista.mercadolivre.com.br");
      super.setUrl(url);
   }
}
