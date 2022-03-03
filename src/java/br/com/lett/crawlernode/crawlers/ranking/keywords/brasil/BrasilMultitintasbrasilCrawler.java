package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilMultitintasbrasilCrawler extends CrawlerRankingKeywords {

   public BrasilMultitintasbrasilCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.log("Página " + this.currentPage);
      this.pageSize = 16;

      String url = "https://multitintasbrasil.com.br/loja/index.php?route=product/search&search=" + this.keywordEncoded + "&page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".product-layout");

      if (!products.isEmpty()) {
         for (Element e : products) {

            String productUrl = CrawlerUtils.scrapUrl(e, ".image a", "href", "https", "www.multitintasbrasil.com.br");
            String internalPid = scrapInternalPid(productUrl);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, "h4", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".image img", Collections.singletonList("src"), "https", "www.multitintasbrasil.com.br");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".price-new", null, true, ',', session, 0);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
               .setInternalId(null)
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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

   private String scrapInternalPid(String productUrl) {
      String internalPid = null;

      if(productUrl != null && !productUrl.isEmpty()) {
         String regex = "product_id=(.*)&search";
         Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
         final Matcher matcher = pattern.matcher(productUrl);

         if(matcher.find()) {
            internalPid = matcher.group(1);
         }
      }

      return internalPid;
   }

   @Override
   public boolean hasNextPage() {
      Elements pagination = this.currentDoc.select(".pagination li");
      for (Element e : pagination) {
         String nextPage = e.toString();
         return nextPage.contains(">");
      }

      return false;

   }

}
