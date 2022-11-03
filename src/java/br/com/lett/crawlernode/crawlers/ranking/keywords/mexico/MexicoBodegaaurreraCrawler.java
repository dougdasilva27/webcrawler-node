package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
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

import java.util.Arrays;

public class MexicoBodegaaurreraCrawler extends CrawlerRankingKeywords {

   public MexicoBodegaaurreraCrawler(Session session) {
      super(session);
   }

   private JSONObject fetchJSON() {
      String keyword = this.keywordEncoded.replace(" ", "+");
      String url = "https://www.bodegaaurrera.com.mx/api/v2/page/search?Ntt=" + keyword + "&size=48&page=" + (this.currentPage - 1) + "&siteId=bodega";
      this.log("Link onde são feitos os crawlers: " + url);
      Request request = Request.RequestBuilder.create()
         .setCookies(cookies)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
            ProxyCollection.SMART_PROXY_MX_HAPROXY))
         .setUrl(url)
         .build();

      Response response = dataFetcher.get(session, request);

      return CrawlerUtils.stringToJson(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      JSONObject json = fetchJSON();

      JSONArray results = JSONUtils.getValueRecursive(json, "appendix.SearchResults.content", JSONArray.class);

      if (results != null && !results.isEmpty()) {
         if (currentPage == 1) {
            this.totalProducts = JSONUtils.getValueRecursive(json, "appendix.SearchResults.totalElements", Integer.class);
         }

         for (Object prod : results) {
            if (prod instanceof JSONObject) {
               JSONObject product = (JSONObject) prod;

               String internalPid = product.optString("id");
               String productUrl = "https://www.bodegaaurrera.com.mx" + product.optString("productSeoUrl");
               String name = product.optString("skuDisplayName");
               String imageUrl = CrawlerUtils.completeUrl(JSONUtils.getValueRecursive(product, "imageUrls.large", String.class), "https", "res.cloudinary.com/walmart-labs/image/upload/w_225,dpr_auto,f_auto,q_auto:good/mg");
               int price = product.optInt("skuPrice");
               boolean isAvailable = price != 0;

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(null)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setPriceInCents(price)
                  .setAvailability(isAvailable)
                  .setImageUrl(imageUrl)
                  .build();

               saveDataProduct(productRanking);
               if (this.arrayProducts.size() == productsLimit)
                  break;
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

}
