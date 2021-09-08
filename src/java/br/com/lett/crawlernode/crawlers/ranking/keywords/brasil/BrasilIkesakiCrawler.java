package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilIkesakiCrawler extends CrawlerRankingKeywords {

   public BrasilIkesakiCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 48;
      this.log("Página " + this.currentPage);

      Document document = fetch();
      Elements products = document.select(".venus li[layout]");

      if (products != null && !products.isEmpty()) {
         for (Element product : products) {

            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product,"[data-id]","data-id");
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product,"a","href");

            saveDataProduct(null, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }

         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private void setTotalProducts(JSONObject json) {
      this.totalProducts = json.optInt("size", 0);
      this.log("Total da busca: " + this.totalProducts);
   }

   private Document fetch() {
      String url = "https://www.ikesaki.com.br/"+this.keywordEncoded+"#" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);


      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .build();

      String response = dataFetcher.get(session, request).getBody();
      return Jsoup.parse(response);
   }
}
