package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;

public class ArgentinaJumboCrawler extends CrawlerRankingKeywords {

   public ArgentinaJumboCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.log("Página " + this.currentPage);

      this.pageSize = 18;

      String url = "https://www.jumbo.com.ar/busca/?ft=" + this.keywordWithoutAccents.replace(" ", "%20") + "&PageNumber=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url, cookies);

      Elements products = this.currentDoc.select("li[layout] .product-item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0)
            setTotalProducts();

         for (Element e : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-item", "data-id");
            String productUrl = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-item__name a", "href"), "https:", "www.jumbo.com.ar");
            ;
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-item__name a", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-item__image-link img", "src");
            int price = CommonMethods.doublePriceToIntegerPrice(CrawlerUtils.scrapDoublePriceFromHtml(e, ".product-prices__value.product-prices__value--best-price", null, true, ',', session), 0);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(null)
               .setInternalPid(internalPid)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
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
      Element totalElement = this.currentDoc.selectFirst(".resultado-busca-numero");

      if (totalElement != null) {
         String text = totalElement.text().replaceAll("[^0-9]", "").trim();

         if (!text.isEmpty()) {
            this.totalProducts = Integer.parseInt(text);
         }

         this.log("Total da busca: " + this.totalProducts);
      }
   }

   private String crawlInternalPid(Element e) {
      return e.attr("data-id");
   }

   private String crawlProductUrl(Element e) {
      String productUrl = null;
      Element urlElement = e.selectFirst(".product-item__name a");

      if (urlElement != null) {
         productUrl = CrawlerUtils.sanitizeUrl(urlElement, Arrays.asList("href"), "https:", "www.jumbo.com.ar");
      }

      return productUrl;
   }
}
