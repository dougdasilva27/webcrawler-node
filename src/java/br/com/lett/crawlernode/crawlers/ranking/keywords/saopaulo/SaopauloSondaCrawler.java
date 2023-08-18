package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

import java.util.Arrays;

public class SaopauloSondaCrawler extends CrawlerRankingKeywords {
   String locate;

   public SaopauloSondaCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.APACHE;
      String locateOpt = session.getOptions().optString("LOCATE");
      this.locate = locateOpt.isEmpty() ? "delivery" : locateOpt;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 96;
      this.log("Página " + this.currentPage);

      String url =
         "https://www.sondadelivery.com.br/"+this.locate+"/busca/" + this.keywordWithoutAccents.replace(" ", "%20") + "/" + this.currentPage + "/96/0/";
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".product-list .product[itemtype=\"http://schema.org/Product\"]");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {

            String productUrl = CrawlerUtils.scrapUrl(e, ".product--info > a", "href", "https", "www.sondadelivery.com.br");
            String internalId = productUrl != null ? crawlInternalId(productUrl) : null;
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product--title span", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".product--thumb img", Arrays.asList("data-srcset"), "https", "");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".int span", null, false, ',', session, 0);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, ".search-filter--results strong", null, true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }

   private String crawlInternalId(String url) {
      return CommonMethods.getLast(url.split("\\?")[0].split("/"));
   }
}
