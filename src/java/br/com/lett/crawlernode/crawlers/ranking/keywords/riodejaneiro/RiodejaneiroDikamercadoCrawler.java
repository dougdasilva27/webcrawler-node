package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;


import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;

public class RiodejaneiroDikamercadoCrawler extends CrawlerRankingKeywords {

   public RiodejaneiroDikamercadoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 20;
      this.log("Página " + this.currentPage);

      String url = "https://dikamercado.com.br/loja/busca/?q=" + this.keywordEncoded + "&page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".col-xl-3");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts(this.currentDoc);
         }
         for (Element e : products) {
            String productUrl = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".media a", "href"), "https", "dikamercado.com.br");
            String internalId = productUrl != null ? CommonMethods.getLast(productUrl.split("-")).replace("/", "") : null;
            String name = CrawlerUtils.scrapStringSimpleInfo(e, "span.nome", false);
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "input.valor-produto-peso", "value", false, ',', session, null);
            String image = CrawlerUtils.scrapSimplePrimaryImage(e, ".media img", List.of("data-src"), "https", "us-southeast-1.linodeobjects.com");
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(image)
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

   private void setTotalProducts(Document doc) {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(doc, ".col-sm-8 h1", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }
}
