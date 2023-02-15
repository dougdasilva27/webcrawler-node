package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MexicoFerreterialalibraCrawler extends CrawlerRankingKeywords {


   public MexicoFerreterialalibraCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 10;

      this.log("Página " + this.currentPage);

      String url = "https://ferreterialalibra.com/tienda/page/" + this.currentPage + "/?s=" + this.keywordEncoded + "&ct_post_type=post%3Apage%3Aproduct";
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".entries article");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalPid = crawlInternaPid(e);
            String productUrl = CrawlerUtils.scrapUrl(e, ".entry-title a", "href", "https", "ferreterialalibra.com/");
            String imgUrl = CrawlerUtils.scrapUrl(e, ".ct-image-container.boundless-image", "href", "https", "ferreterialalibra.com/");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".entry-title a", true);

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(null)
               .setInternalPid(internalPid)
               .setImageUrl(imgUrl)
               .setName(name)
               .setPriceInCents(null)  //this site have not price in the search page
               .setAvailability(false)
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


   private String crawlInternaPid(Element e) {
      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, null, "id");
      if (internalId != null) {
         internalId = internalId.split("-")[1];
      }
      return internalId;
   }

   @Override
   protected boolean hasNextPage() {
      return this.currentDoc.select(".next.page-numbers").size() > 0;
   }
}
