package br.com.lett.crawlernode.crawlers.ranking.keywords.manaus;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.VtexRankingPidPosition;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Document;

public class ManausSupernovaeraCrawler extends VtexRankingPidPosition {

   public ManausSupernovaeraCrawler(Session session) {
      super(session);
   }

   protected final String storeId = getStoreId();

   protected String getStoreId() {
      return session.getOptions().optString("store_id");
   }

   @Override
   protected String getHomePage() {
      return session.getOptions().optString("home_page");
   }

   @Override
   protected String getUrlParams() {
      return session.getOptions().optString("url_params");
   }

   @Override
   protected void setTotalProducts() {
      Document html = fetchDocument(homePage + keywordEncoded + "?sc=" + storeId);
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(html, ".resultado-busca-numero span.value", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }


}
