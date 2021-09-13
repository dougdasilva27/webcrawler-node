package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilClimarioCrawler extends CrawlerRankingKeywords {

   protected String searchPageUrl;

   public BrasilClimarioCrawler(Session session) {
      super(session);
   }

   private String getNextPageUrl() {
      String url = "";
      Pattern regexAppToken = Pattern.compile(".load\\('(/buscapagina\\?.*?)'");

      Matcher matcher = regexAppToken.matcher(this.currentDoc.toString());

      if (matcher.find()) {
         url = matcher.group(1);
      }

      return url;
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 18;
      this.log("Página " + this.currentPage);

      String keyword = this.keywordEncoded.replace("+", "%20");

      String url = "";
      if (this.currentPage == 1) {
         url = "https://www.climario.com.br/" + keyword + "?&utmi_pc=BuscaFullText";
         this.currentDoc = fetchDocument(url);
         searchPageUrl = getNextPageUrl();
      } else {
         url = "https://www.climario.com.br" + searchPageUrl + this.currentPage;
         this.currentDoc = fetchDocument(url);
      }


      this.log("Link onde são feitos os crawlers: " + url);
      Elements products = this.currentDoc.select(".vitrine li[layout]");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div.yv-review-quickreview", "value");
            String urlProduct = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a.product-image", "href");

            //For some reason, the first product is not present in the page and has no id
            if (internalPid != null) {
               saveDataProduct(null, internalPid, urlProduct);

               this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
               if (this.arrayProducts.size() == productsLimit) break;
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }
}
