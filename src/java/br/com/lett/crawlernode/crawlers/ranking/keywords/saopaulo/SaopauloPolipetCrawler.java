package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.Arrays;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class SaopauloPolipetCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "www.polipet.com.br";

   public SaopauloPolipetCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      // Quantidade de produtos por página do market
      this.pageSize = 20;

      this.log("Página " + this.currentPage);

      // Monta a url com a keyword e a página
      String url = "https://www.polipet.com.br/busca?busca=" + this.keywordEncoded + "&pagina=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);

      // Chama a função a qual pega o html
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".fbits-item-lista-spot");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         for (Element e : products) {
            String internalid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".spot", "id").split("-")[3];
            String productUrl = CrawlerUtils.scrapUrl(e, ".spotContent .spot-parte-um", "href", "https:", HOME_PAGE);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".spotTitle", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".spotImg > img", "data-original");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".fbits-valor", null, true, ',', session, 0);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalid)
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
   protected boolean hasNextPage() {
      return this.currentDoc.selectFirst(".fbits-paginacao ul .pg a") != null;
   }
}

