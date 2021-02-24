package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;

public class MexicoJustoCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "justo.mx";
   private static final String POSTAL_CODE = "14300";

   public MexicoJustoCrawler(Session session) {
      super(session);
   }

   @Override
   public void processBeforeFetch() {
      BasicClientCookie cookie = new BasicClientCookie("postal_code", POSTAL_CODE);
      cookie.setDomain("justo.mx");
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 15;
      this.log("Página " + this.currentPage);

      String url = "https://justo.mx/search/?q=" + this.keywordEncoded + "&page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".product-card");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div .product-card__content > div", "data-id");
            String productUrl = CrawlerUtils.scrapUrl(e, "div .product-card__content > a", Arrays.asList("href"), "https", HOME_PAGE);

            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null
               + " - Url: " + productUrl);

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }

      }else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");

   }

   protected boolean hasNextPage() {
      Element page = this.currentDoc.selectFirst(".last.page-item.disabled");
      return page == null;
   }

}
