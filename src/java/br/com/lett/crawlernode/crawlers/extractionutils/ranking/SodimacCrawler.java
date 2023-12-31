package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Collections;

public class SodimacCrawler extends CrawlerRankingKeywords {
   public SodimacCrawler(Session session) {
      super(session);
   }

   private Boolean isCategory = false;
   private String urlCategory = null;

   public String getBaseUrl() {
      return session.getOptions().optString("baseUrl");
   }

   public char getPriceFormat() {
      return session.getOptions().optString("priceFormat").charAt(0);
   }

   @Override
   public void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 28;
      String url = getBaseUrl() + "search?Ntt="+this.keywordEncoded+"&currentpage=" + this.currentPage;
      if(this.currentPage > 1 && isCategory) {
         url = urlCategory + "&currentpage=" + this.currentPage;
      }

      this.currentDoc = fetchDocument(url);

      if(this.currentPage == 1) {
         String redirectUrl = this.session.getRedirectedToURL(url);

         if(redirectUrl != null && !redirectUrl.equals(url)) {
            isCategory = true;
            this.urlCategory = redirectUrl;
         } else {
            isCategory = false;
         }
      }

      Elements products = this.currentDoc.select(".search-results-products-container > div");
      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapUrl(e, "#title-pdp-link", "href", "https", session.getOptions().optString("homePage"));
            String productId = e.attr("data-key");
            String productPid = productId;
            String brand = CrawlerUtils.scrapStringSimpleInfo(e, "a .product-brand", true);
            String name = brand + " - " + CrawlerUtils.scrapStringSimpleInfo(e, ".product-title", true);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".product-image > div > img", Collections.singletonList("data-src"), "https", "sodimac.scene7.com");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".price", null, false, getPriceFormat(), session, null);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(productId)
               .setInternalPid(productPid)
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
   protected boolean hasNextPage() {
      return this.currentDoc.selectFirst("#top-pagination-next-page") != null;
   }
}
