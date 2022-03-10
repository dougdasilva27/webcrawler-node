package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

import java.util.Collections;

public class DrogariaMinasbrasilNetCrawler extends CrawlerRankingKeywords {

   protected String host;

   public DrogariaMinasbrasilNetCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 16;
      this.log("Página " + this.currentPage);

      String url = "https://" + this.host + "/catalogsearch/result/?q=" + this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".products .product");

      if (!products.isEmpty()) {
         if (totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalPid = null;
            String internalId = crawlInternalId(e);
            String productUrl = CrawlerUtils.scrapUrl(e, ".product.-list .product-picture a", "href", "https", this.host);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-name", true);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".product-picture img", Collections.singletonList("data-src"), "https", "drogariaminasbrasil.com.br");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".product-price", null, false, ',', session, null);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
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
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, ".toolbar-count span", true, 0);
      this.log("Total products: " + this.totalProducts);
   }

   private String crawlInternalId(Element e) {
      String internalId = null;
      Element idElement = e.selectFirst(".buy-form");

      if (idElement != null) {
         String url = idElement.attr("action");

         if (url.contains("product/")) {
            internalId = CommonMethods.getLast(url.split("product/")).split("/")[0].trim();
         }
      }

      return internalId;
   }
}
