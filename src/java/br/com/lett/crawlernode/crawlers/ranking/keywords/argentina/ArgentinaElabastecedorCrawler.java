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

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class ArgentinaElabastecedorCrawler extends CrawlerRankingKeywords {
   private int totalPages = 0;
   private static final String HOME_PAGE = "https://www.elabastecedor.com.ar/";

   public ArgentinaElabastecedorCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 20;

      this.log("Página " + this.currentPage);

      String url = "https://www.elabastecedor.com.ar/resultado.php?search=" + this.keywordEncoded + "&page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);
      this.totalPages = this.currentDoc.select(".pagination li").size();
      Elements products = this.currentDoc.select("#shop-1 .row .col-xl-2");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {
            String productUrl = crawlProductUrl(e);
            String internalId = getInternalId(productUrl);
            String internalPid = internalId;
            String name = CrawlerUtils.scrapStringSimpleInfo(e,".nombreProducto span", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".img-block img", Arrays.asList("src"), "https", "");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".current-price", null, false, ',', session, 0);
            boolean isAvailable = price != 0;

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

            if (this.arrayProducts.size() == productsLimit) break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   private String crawlProductUrl(Element e) {
      String url = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".inner-link ", "href");
      return CrawlerUtils.completeUrl(url,"https:","www.elabastecedor.com.ar");
   }

   private String getInternalId(String url) {
         String [] urlId = url.split("_");

         return urlId[0].replaceAll("[^0-9]", "");
   }

   @Override
   protected boolean hasNextPage() {
      return this.currentPage < this.totalPages;
   }
}
