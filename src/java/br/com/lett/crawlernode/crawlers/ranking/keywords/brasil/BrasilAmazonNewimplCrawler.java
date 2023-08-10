package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;

public class BrasilAmazonNewimplCrawler extends BrasilAmazonCrawler {

   public BrasilAmazonNewimplCrawler(Session session) {
      super(session);
   }

   private String marketplaceId = session.getOptions().optString("marketplaceId", "");

   @Override
   protected String getUrl() {
      return "https://www.amazon.com.br/s?k=" + this.keywordEncoded + "&me=" + marketplaceId;
   }
}
