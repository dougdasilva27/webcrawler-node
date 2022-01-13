package br.com.lett.crawlernode.crawlers.ranking.keywords.belohorizonte;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.InternalIdNotFound;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;

public class BelohorizonteSantahelenaCrawler extends CrawlerRankingKeywords {

   public BelohorizonteSantahelenaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 16;

      this.log("Página " + this.currentPage);

      String url = "https://santahelenacenter.com.br/page/" + this.currentPage + "/?s=" + this.keywordEncoded + "&post_type=product&dgwt_wcas=1";
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".products.columns-4 li");

      for (Element e : products) {
         String name = CrawlerUtils.scrapStringSimpleInfo(e, "h3", true);
         String productUrl = CrawlerUtils.scrapUrl(e, ".container-inner a", "href", "https", "santahelenacenter.com.br");
         String internalId = e.classNames().stream().filter(s -> s.matches("post-[0-9^]*")).findFirst()
            .map(s -> s.replaceAll("[^0-9]", ""))
            .orElseThrow(InternalIdNotFound::new);
         Integer priceInCents = (int) Math.round(CrawlerUtils.scrapDoublePriceFromHtml(e, ".price", null, false, ',', session) * 100);
         String image = CrawlerUtils.scrapSimplePrimaryImage(e, "img", Arrays.asList("src"), "https", "santahelenacenter.com.br");
         boolean available = e.selectFirst("form") != null;

         try {
            RankingProduct rankingProduct = RankingProductBuilder.create()
               .setName(name)
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setImageUrl(image)
               .setAvailability(available)
               .setPriceInCents(priceInCents)
               .build();

            saveDataProduct(rankingProduct);
         } catch (MalformedProductException error) {
            this.log(error.getMessage());
         }

         if (this.arrayProducts.size() == productsLimit) {
            break;
         }
      }
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".next.page-numbers").isEmpty();
   }
}
