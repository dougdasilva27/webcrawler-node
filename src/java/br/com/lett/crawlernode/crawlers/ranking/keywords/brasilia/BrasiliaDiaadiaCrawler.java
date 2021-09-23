package br.com.lett.crawlernode.crawlers.ranking.keywords.brasilia;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.models.RankingProducts;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasiliaDiaadiaCrawler extends CrawlerRankingKeywords {

   public BrasiliaDiaadiaCrawler(Session session) {
      super(session);
   }

   @Override
   protected Document fetchDocument(String url) {
      this.log("Link onde são feitos os crawlers: " + url);

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .build();

      Response response = dataFetcher.get(session, request);
      return Jsoup.parse(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      String url;

      if(this.currentPage == 1){
         url = "https://www.diaadiaonline.com.br/" + this.keywordEncoded.replace("+", "%20");
      }else{
         url = "https://www.diaadiaonline.com.br/buscapagina?&ft=" + this.keywordEncoded + "&PS=16&sl=5983edad-85da-4f1b-87fa-67e8450c1d59&cc=16&sm=0&PageNumber=" + this.currentPage;
      }

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("div.shelf__product-item");

      if (products != null && !products.isEmpty()) {
         if (currentPage == 1) {
            this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, "span.resultado-busca-numero > span.value", true, 0);
         }

         for (Element product : products) {
            String internalPid = product.attr("data-product-id");
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "input[type=checkbox]", "name");
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "a.product-item__img", "href");
            String name = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "a.product-item__img", "title");
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "div.product-item__img__default img", "src");
            int price = CrawlerUtils.scrapIntegerFromHtml(product, "div.product-item__best-price > span", true, 0);
            boolean isAvailable = price != 0;

            //New way to send products to save data product
            RankingProducts productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
               .build();

            saveDataProduct(productRanking);
            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage(){
      return true;
   }
}
