package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class BrasilFarmCrawler extends CrawlerRankingKeywords {

   public BrasilFarmCrawler(Session session) {
      super(session);
   }


   protected JSONObject fetchJSONObject() {
      JSONObject json = new JSONObject();

      String url = "https://busca.farmrio.com.br/busca?q="+this.keywordEncoded+"&page="+this.currentPage+"&ajaxSearch=1";

      Request request =  Request.RequestBuilder.create().setUrl(url).build();
      String response = this.dataFetcher.get(session,request).getBody();

      if ( response != null && !response.isEmpty()){
         json = CrawlerUtils.stringToJson(response);
      }

      return json;
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);

      String url = "https://busca.farmrio.com.br/busca?q=" + this.keywordEncoded;

      this.currentDoc = fetchDocument(url);
      this.log("Link onde são feitos os crawlers: " + url);

      JSONObject json = fetchJSONObject();
      JSONObject productsInfo = json.optJSONObject("productsInfo");
      JSONArray  products = productsInfo.optJSONArray("products");

      if (!products.isEmpty()) {

         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Object o : products) {

            JSONObject product = (JSONObject) o;

            String productUrl = CrawlerUtils.completeUrl(product.optString("productUrl"), "https:", "www.farmrio.com.br/");
            String internalPid = product.optString("originalId");

            saveDataProduct(null, internalPid, productUrl);

            this.log(
               "Position: " + this.position +
                  " - InternalId: " + null +
                  " - InternalPid: " + internalPid +
                  " - Url: " + productUrl);

            if (this.arrayProducts.size() == productsLimit)
               break;
         }

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".neemu-total-products-container",false,0);

      this.log("Total da busca: "+this.totalProducts);
   }

}
