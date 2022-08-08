package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

import java.util.Collections;

public class MexicoHebCrawler extends CrawlerRankingKeywords {

   public MexicoHebCrawler(Session session) {
      super(session);
   }

   private String categoryUrl;

   private String getStore() {
      return this.session.getOptions().optString("store");
   }

   @Override
   protected void processBeforeFetch() {
      BasicClientCookie cookie = new BasicClientCookie("store", getStore());
      cookie.setDomain("www.heb.com.mx");
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 56;
      this.log("Página " + this.currentPage);

      String url = "https://www.heb.com.mx/catalogsearch/result/index/?limit=36&p=" + this.currentPage + "&q=" + this.keywordEncoded;

      if (this.currentPage > 1 && this.categoryUrl != null) {
         url = this.categoryUrl + "?p=" + this.currentPage;
      }

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".products-grid .item .product-item-info");

      if (this.currentPage == 1) {
         String redirectUrl = CrawlerUtils.getRedirectedUrl(url, session);

         if (!url.equals(redirectUrl)) {
            this.categoryUrl = redirectUrl;
         }
      }

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e,  "> a", "data-product_id");
            String productUrl = CrawlerUtils.scrapUrl(e, "> a", Collections.singletonList("href"), "https", "heb.com.mx");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-item-name", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".product-image-photo", Collections.singletonList("src"), "https", "heb.com.mx");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "[data-price-type=finalPrice]", null, false, '.', session, 0);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(null)
               .setInternalPid(internalPid)
               .setImageUrl(imgUrl)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);
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
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, "#toolbar-amount .toolbar-number:last-child", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }
}
