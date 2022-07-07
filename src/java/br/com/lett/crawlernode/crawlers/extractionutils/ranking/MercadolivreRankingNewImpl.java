package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.session.Session;

public class MercadolivreRankingNewImpl extends MercadolivreCrawler{

   private final String URL = session.getOptions().optString("homePage") + this.keywordWithoutAccents.replace(" ", "-") + session.getOptions().optString("id");
   private final String PRODUCT_URL_HOST = session.getOptions().optString("product_url_host", "produto.mercadolivre.com.br");
   private final String NEXT_URL_HOST = session.getOptions().optString("next_url_host", "lista.mercadolivre.com.br");

   public MercadolivreRankingNewImpl(Session session) {
      super(session);
      super.setUrl(URL);
      super.setProductUrlHost(PRODUCT_URL_HOST);
      super.setNextUrlHost(NEXT_URL_HOST);
   }
}
