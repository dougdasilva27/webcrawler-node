package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SaopauloVarandaCrawler extends CrawlerRankingKeywords {

   public SaopauloVarandaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 30;

      this.log("Página " + this.currentPage);
      String url = "https://www.varanda.com.br/catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordWithoutAccents;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".products-grid.category-products-grid.itemgrid.itemgrid-adaptive.itemgrid-6col li");

      if (!products.isEmpty()) {

         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapUrl(e, "a", "href", "https", "www.varanda.com.br");
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "button", "data-id");
            String name = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-name a", "title");
            String imgUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".imagem1", "src");
            Integer price = getPrice(e);
            boolean isAvailable = isAvailability(price) ;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();
            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   private boolean isAvailability(Integer price) {
      if(price!=null && price >0){
         return true;
      }
      return false;
   }

   private Integer getPrice(Element e) {
      Double price = CrawlerUtils.scrapDoublePriceFromHtml(e, ".parcelaBloco", "data-valor_produto", true, '.', session);
      return price != null ? MathUtils.parseInt(price * 100) : 0;
   }



   @Override
   protected boolean hasNextPage() {
      Elements textsPages = this.currentDoc.select(".final.disable");
      if(textsPages== null || textsPages.isEmpty()){
         return true;
      }
      return false;
   }
}
