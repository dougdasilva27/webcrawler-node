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

public class BrasilEspacoprimeCrawler extends CrawlerRankingKeywords {

   public BrasilEspacoprimeCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 27;

      String url = "https://www.espacoprime.com.br/busca?busca=" + this.location.replace(" ", "+") + "&pagina=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      this.log("Página " + this.currentPage);
      Elements products = this.currentDoc.select("div.spot");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapUrl(e, ".spot-parte-um", "href", "https", "www.espacoprime.com.br");
            String internalPid = CommonMethods.getLast(productUrl.split("-"));
            String name = CrawlerUtils.scrapStringSimpleInfo(e, "h3.spotTitle", true);
            String imageUrl = crawlImage(e);
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".precoPor .fbits-valor", null, false, ',', session, null);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
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

   private String crawlImage(Element e) {
      String image = CrawlerUtils.scrapSimplePrimaryImage(e, ".spotImg img.imagem-primaria", Collections.singletonList("data-original"), "https", "espacoprime.fbitsstatic.net");
      if (image != null) {
         return image.split("\\?")[0];
      }
      return image;
   }

   @Override
   protected void setTotalProducts() {
      Integer total = CrawlerUtils.scrapSimpleInteger(this.currentDoc, ".mostrando.left .fbits-qtd-produtos-pagina", true);
      if (total == null) {
         total = CrawlerUtils.scrapSimpleInteger(this.currentDoc, ".fbits-qtd-produtos-pagina", true);
      }
      this.totalProducts = total;
   }
}
