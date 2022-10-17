package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.Collections;

public class BrasilFemalepetCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "https://www.femalepet.com.br";

   public BrasilFemalepetCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);

      String url = HOME_PAGE + "/buscapagina?ft=" + this.keywordEncoded + "&PS=12&sl=903db33a-2b87-48c6-864b-d2ab5cacf565&cc=12&sm=0&PageNumber=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select("ul > li:not(.helperComplement)");

      if (!products.isEmpty()) {
         for (Element e : products) {

            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".shelf__item", "data-productid");
            String productUrl = CrawlerUtils.scrapUrl(e, ".shelf__item--name", Arrays.asList("href"), "https", HOME_PAGE);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".shelf__item--name", true);
            String image = CrawlerUtils.scrapSimplePrimaryImage(e, ".shelf__item--image img", Collections.singletonList("src"), "https", "femalepet.vteximg.com.br");
            Integer priceInCents = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".shelf__item--price__best", null, false, ',', session, 0);
            boolean available = priceInCents != 0;


            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(null)
               .setInternalPid(internalPid)
               .setImageUrl(image)
               .setName(name)
               .setPriceInCents(priceInCents)
               .setAvailability(available)
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
   protected boolean hasNextPage() {
      Integer productCount = this.currentDoc.select("ul > li:not(.helperComplement)").size();
      return productCount >= this.pageSize;
   }
}
