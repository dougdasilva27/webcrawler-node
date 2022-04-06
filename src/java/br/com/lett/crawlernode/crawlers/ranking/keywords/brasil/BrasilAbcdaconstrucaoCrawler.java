package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Collections;

public class BrasilAbcdaconstrucaoCrawler extends CrawlerRankingKeywords {

   public BrasilAbcdaconstrucaoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {

      this.pageSize = 24;

      this.log("Página " + this.currentPage);

      String url = "https://www.abcdaconstrucao.com.br/busca?busca=" + this.keywordEncoded + "&pagina=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);


      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".spots-interna .fbits-item-lista-spot");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String rawInternal = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".spot", "id");
            String internalId = rawInternal != null && rawInternal.contains("produto-spot-item-") ? rawInternal.split("item-")[1] : null;
            String productUrl = CrawlerUtils.scrapUrl(e, ".spot-parte-um", "href", "https:", "www.abcdaconstrucao.com.br");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".spotTitle", true);
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".spotImg img", Collections.singletonList("data-original"), "https", "www.abcdaconstrucao.com.br");
            Integer price = CrawlerUtils.scrapIntegerFromHtml(e, ".fbits-valor", true, null);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(null)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
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
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".fbits-qtd-produtos-pagina", true, 0);
      this.log("Total: " + this.totalProducts);
   }

}
