package br.com.lett.crawlernode.crawlers.ranking.keywords.portugal;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;

public class PortugalAuchanCrawler extends CrawlerRankingKeywords {

   public PortugalAuchanCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.APACHE;
   }

   @Override
   protected void processBeforeFetch() {
      super.processBeforeFetch();
      Logging.printLogDebug(logger, session, "Adding cookie...");

      this.cookies = CrawlerUtils
         .fetchCookiesFromAPage("https://www.auchan.pt/Frontoffice/", Arrays.asList("ASP.NET_SessionId", "AuchanSessionCookie", "AUCHANCOOKIE"), "www.auchan.pt", "/", cookies, session, null,
            dataFetcher);
   }

   public Document fetch(String url) {
      String doc = "";
      Request request = Request.RequestBuilder.create().setCookies(cookies).setUrl(url).build();
      doc = this.dataFetcher.get(session, request).getBody();

      return Jsoup.parse(doc);
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      this.pageSize = 30;
      this.log("Página " + this.currentPage);

      String url = "https://www.auchan.pt/Frontoffice/search/" + this.keywordWithoutAccents.replace(" ", "%20");

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetch(url);
      Elements products = this.currentDoc.select(".product-item");


      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalId = e.attr("data-product-id");
            String productUrl = "https://www.auchan.pt" + CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-item-header a", "href");
            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit)
               break;

         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, "#page .col-sm-9 h3", false, 0);

      this.log("Total de produtos: " + this.totalProducts);
   }

}
