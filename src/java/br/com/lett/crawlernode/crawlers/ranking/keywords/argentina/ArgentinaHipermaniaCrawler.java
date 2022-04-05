package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.InternalIdNotFound;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Collections;

public class ArgentinaHipermaniaCrawler extends CrawlerRankingKeywords {

   public ArgentinaHipermaniaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 16;

      this.log("Página " + this.currentPage);

      String url = "https://www.hipermania.com.ar/page/" + this.currentPage + "/?s=" + this.keywordEncoded + "&post_type=product";
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".products.columns-4 li");

      for (Element e : products) {
         String productUrl = CrawlerUtils.scrapUrl(e, ".woocommerce-LoopProduct-link", "href", "https", "www.hipermania.com.ar");
         String internalId = scrapInternalId(e);
         String name = CrawlerUtils.scrapStringSimpleInfo(e, ".woocommerce-loop-product__title", true);
         String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".attachment-woocommerce_thumbnail", Collections.singletonList("src"), "https", "www.hipermania.com.ar");
         Integer price = scrapPrice(e);
         boolean isAvailable = price != null;

         RankingProduct productRanking = RankingProductBuilder.create()
            .setUrl(productUrl)
            .setInternalId(internalId)
            .setInternalPid(null)
            .setImageUrl(imgUrl)
            .setName(name)
            .setPriceInCents(price)
            .setAvailability(isAvailable)
            .build();

         saveDataProduct(productRanking);
      }
   }

   private Integer scrapPrice(Element e) {
      Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".price > ins .amount", null, false, '.', session, null);

      if(price == null) {
         price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".price .amount", null, false, '.', session, null);
      }

      return price;
   }

   private String scrapInternalId(Element e) {
      return e.classNames().stream().filter(s -> s.matches("post-[0-9^]*")).findFirst()
         .map(s -> s.replaceAll("[^0-9]", ""))
         .orElseThrow(InternalIdNotFound::new);
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".next.page-numbers").isEmpty();
   }
}
