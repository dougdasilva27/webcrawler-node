package br.com.lett.crawlernode.crawlers.ranking.keywords.peru;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class PeruFazilCrawler extends CrawlerRankingKeywords {
   public PeruFazilCrawler(Session session) {
      super(session);
   }


   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 20;
      this.log("Página " + this.currentPage);

      String url = "https://www.tottus.com.pe/s/product-search/v2/search?page=" + (this.currentPage - 1) + "&perPage=20&sort=score%2Cdesc&text=" + this.keywordWithoutAccents + "&filter=attributes.sellerid%3A20508565934&filter=inStock%3AYa_LaFontana&groupenabled=true&groupby=productId&abFlags=default";

      this.log("Link onde são feitos os crawlers: " + url);
      JSONObject jsonRanking = fetchJsonAPi(url);
      JSONArray productsArray = jsonRanking.optJSONArray("results");

      this.totalProducts = JSONUtils.getValueRecursive(jsonRanking, "pagination.totalProducts", Integer.class);
      this.log("Total da busca: " + productsArray.length());

      if (productsArray != null && !productsArray.isEmpty()) {
         for (Object productObj : productsArray) {
            JSONObject product = (JSONObject) productObj;

            String internalPid = product.optString("sku");
            String internalId = product.optString("productId");
            String productUrl = product.optString("key");
            String name = JSONUtils.getValueRecursive(product, "name.es-PE", String.class);
            String imageUrl = JSONUtils.getValueRecursive(product, "images.0", String.class);
            Integer price = null;
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
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

   protected JSONObject fetchJsonAPi(String url) {

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY
         ))
         .build();
      Response response = CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher(), true);
      return JSONUtils.stringToJson(response.getBody());
   }
}
