package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SaopauloTrimaisCrawler extends CrawlerRankingKeywords {

   public SaopauloTrimaisCrawler(Session session) {
      super(session);
   }

   private Document fetchApi(){

      StringBuilder searchApi = new StringBuilder();

      searchApi.append("https://www.trimais.com.br/")
         .append(this.keywordEncoded)
         .append("?PageNumber=")
         .append(this.currentPage);

      String apiUrl = searchApi.toString().replace("+", "%20");

      this.log("Link onde são feitos os crawlers: " + searchApi.toString());

      Request request = Request.RequestBuilder.create().setUrl(apiUrl).build();

      return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      this.log("Página " + this.currentPage);

      this.currentDoc = fetchApi();

      Elements products = this.currentDoc.select(".prateleira ul li .product-big");

      if(products.size() > 0){

         if(this.totalProducts == 0){
            this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".resultado-busca-numero span.value", true, 0);
            this.log("Total da busca: " + this.totalProducts);
         }

         for(Element product:products){

            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, null, "item");

            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, null, "rel");

            String urlProduct = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "figure a", "href");

            saveDataProduct(internalId, internalPid, urlProduct);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
            if (this.arrayProducts.size() == productsLimit) break;
         }

      } else{
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return arrayProducts.size() < this.totalProducts;
   }
}
