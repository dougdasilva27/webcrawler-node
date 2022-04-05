package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

import java.util.Collections;

public class BrasilCobasiCrawler extends CrawlerRankingKeywords {

   public BrasilCobasiCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 35;

      this.log("Página " + this.currentPage);

      // monta a url com a keyword e a página
      String url = "https://busca.cobasi.com.br/busca?q=" + this.keywordEncoded + "&page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".neemu-products-container .nm-product-item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalPid = crawlInternalPid(e);
            String productUrl = crawlProductUrl(e);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".nm-product-name", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".nm-product-img", Collections.singletonList("src"), "https", "cobasi.com.br");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".nm-price-container .nm-price-value", null, true, ',', session, 0);
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
      Element totalElement = this.currentDoc.select(".neemu-total-products-container").first();

      if (totalElement != null) {
         String total = totalElement.ownText().replaceAll("[^0-9]", "").trim();

         if (!total.isEmpty()) {
            this.totalProducts = Integer.parseInt(total);
         }

         this.log("Total da busca: " + this.totalProducts);
      }
   }

   private String crawlInternalPid(Element e) {
      return CommonMethods.getLast(e.attr("id").split("-"));
   }

   private String crawlProductUrl(Element e) {
      String productUrl = null;

      Element url = e.select(".nm-product-info a").first();

      if (url != null) {
         productUrl = url.attr("href");

         if (!productUrl.startsWith("http")) {
            productUrl = "https:" + productUrl;
         }
      }

      return productUrl;
   }
}
