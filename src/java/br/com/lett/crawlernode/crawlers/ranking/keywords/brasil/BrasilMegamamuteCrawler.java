package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilMegamamuteCrawler extends CrawlerRankingKeywords {

   public BrasilMegamamuteCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      //número de produtos por página do market
      this.pageSize = 16;

      this.log("Página " + this.currentPage);

      //monta a url com a keyword e a página
      String url = "https://www.megamamute.com.br/pesquisa?t=" + this.keywordEncoded + "#/pagina-" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      //chama função de pegar a url
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".wd-browsing-grid-list ul li");

      //se obter 1 ou mais links de produtos e essa página tiver resultado faça:
      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         for (Element e : products) {
            // InternalId
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div[data-product-id]", "data-product-id");


            // Url do produto
            String urlProduct = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a", "href");
            String completeUrl = CrawlerUtils.completeUrl(urlProduct, "https", "www.megamamute.com.br");


            //Captura de dados
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".name > a", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".variation-root > img", "data-src");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".sale-price > span", null, true, ',', session, 0);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(completeUrl)
               .setInternalId(internalId)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
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


   @Override
   protected boolean hasNextPage() {
      return this.currentDoc.selectFirst(".page-number > span > a") != null;
   }
}

