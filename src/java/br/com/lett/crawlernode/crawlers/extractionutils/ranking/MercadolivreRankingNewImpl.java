package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.session.Session;

public class MercadolivreRankingNewImpl extends MercadolivreCrawler{

   private final String URL = "https://lista.mercadolivre.com.br/" + this.keywordWithoutAccents.replace(" ", "-") + session.getOptions().optString("id");

   public MercadolivreRankingNewImpl(Session session) {
      super(session);
      super.setUrl(URL);
      super.setProductUrlHost("produto.mercadolivre.com.br");
      super.setNextUrlHost("lista.mercadolivre.com.br");
   }
}
