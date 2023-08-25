package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;

public class ArgentinaCentraloesteCrawler extends CrawlerRankingKeywords {


   private static final String HOME_PAGE = "www.centraloeste.com.ar";

   public ArgentinaCentraloesteCrawler(Session session) {
      super(session);
   }

   private String categoryUrl;

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 20;
      this.log("Página " + this.currentPage);

      String url = crawlUrl();

      if (this.currentPage > 1 && this.categoryUrl != null) {
         url = this.categoryUrl + "?p=" + this.currentPage;
      }

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".item.product.product-item");

      if (this.currentPage == 1) {
         String redirectUrl = CrawlerUtils.getRedirectedUrl(url, session);

         if (!url.equals(redirectUrl)) {
            this.categoryUrl = redirectUrl;
         }
      }

      if (!products.isEmpty()) {

         for (Element e : products) {
            String internalId = e.selectFirst(".product-item-info > .product.details > .price-box").attr("data-product-id");
            String internalPid = e.selectFirst("form.tocart-form").attr("data-product-sku");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product.name.product-item-name", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, "img.product-image-photo", Arrays.asList("src"), "https", HOME_PAGE);
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "span[data-price-type=\"finalPrice\"] > .price", null, false, ',', session, null);
            boolean isAvailable = price != null;
            String productUrl = CrawlerUtils.scrapUrl(e, ".product.photo.product-item-photo", "href", "https", HOME_PAGE);

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");

   }

   private String crawlUrl() {
      String link;

      if (this.currentPage > 1) {
         link = "https://www.centraloeste.com.ar/catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded;
      } else {
         link = "https://www.centraloeste.com.ar/catalogsearch/result/?q=" + this.keywordEncoded;
      }
      return link;
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".item.pages-item-next").isEmpty();
   }

}
