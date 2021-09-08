package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;

public class BrasilZattiniCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "zattini.com.br";
   private String searchUrl = "";


   public BrasilZattiniCrawler(Session session) {
      super(session);
   }


   protected void fetchUrl() {

      String url = "https://www.zattini.com.br/busca?nsCat=Natural&q=" + this.keywordEncoded;

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .build();
      String redirectUrl = this.dataFetcher
         .get(session, request)
         .getRedirectUrl();

      if (redirectUrl != null) {
         searchUrl = redirectUrl;
      } else {
         searchUrl = url;
      }
   }


   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 42;
      this.log("Página " + this.currentPage);

      if (searchUrl.isEmpty()) {
         fetchUrl();
      }

      String url = searchUrl + "&page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("#item-list .wrapper a");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalPid = e.attr("parent-sku");
            String productUrl = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a", "href"), "https","");

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


   @Override
   protected void setTotalProducts() {
      Element totalElement = this.currentDoc.selectFirst(".items-info .block");

      if (totalElement != null) {
         String text = totalElement.ownText().toLowerCase();

         if (text.contains("de")) {
            String total = text.split("de")[1].replaceAll("[^0-9]", "").trim();

            if (!total.isEmpty()) {
               this.totalProducts = Integer.parseInt(total);
            }
         }

         this.log("Total da busca: " + this.totalProducts);
      }
   }


}
