package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrasilEsalpetCrawler extends CrawlerRankingKeywords {

   public BrasilEsalpetCrawler(Session session) {
      super(session);
   }

   private JSONObject fetchApi() {

      String url = "https://esal.api.alleshub.com.br/api_ecommerce/getItensBySearchTerm";

      Map<String, String> headers = new HashMap<>();
      headers.put("BaseConn", "{\"base\":\"erp_esalpet\",\"emp_id\":1}");
      headers.put("Content-Type", "application/json");
      headers.put("Host", "esal.api.alleshub.com.br");
      headers.put("Origin", "https://www.esalpet.com.br");
      headers.put("Referer", "https://www.esalpet.com.br/");
      headers.put("x-requested-with", "XMLHttpRequest");

      String payload = "{\"term\":\"racao\",\"exactMatch\":false}";

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .setProxyservice(List.of(ProxyCollection.BUY, ProxyCollection.NETNUT_RESIDENTIAL_BR, ProxyCollection.SMART_PROXY_BR))
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new JsoupDataFetcher(), new FetcherDataFetcher(), new ApacheDataFetcher()), session, "post");
      return CrawlerUtils.stringToJson(response.getBody());

   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {

      JSONObject json = fetchApi();
      JSONArray products = json != null ? json.optJSONArray("data") : null;

      if (products != null && !products.isEmpty()) {
         for (Object obj : products) {
            if (obj instanceof JSONObject) {
               JSONObject product = (JSONObject) obj;
               String internalId = integerToString(JSONUtils.getValueRecursive(product, "item_linkWeb.item_id", Integer.class));
               String internalPid = integerToString(JSONUtils.getValueRecursive(product, "item_linkWeb.ecommerce_id", Integer.class));
               String name = JSONUtils.getValueRecursive(product, "item_linkWeb.linkWeb.titulo", String.class);
               String productUrl = crawlUrl(name, internalId);
               String imageUrl = crawlImage(product);
               boolean available = product.opt("estoque") != null;
               Integer price = available ? JSONUtils.getValueRecursive(product, "tabela.Vvenda", Integer.class, 0) : null;

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setPriceInCents(price)
                  .setAvailability(available)
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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

   private String crawlUrl(String name, String internalId) {
      String url = "https://www.esalpet.com.br/produto/" + CommonMethods.toSlug(name) + "?id=" + internalId;

      return url;
   }

   private String integerToString(Integer value) {
      return value != null ? value.toString() : null;
   }

   private String crawlImage(JSONObject data) {

      JSONArray imagesArray = data.optJSONArray("fotos");

      if (imagesArray != null) {
         return "https://esal.api.alleshub.com.br/images/erp_esalpet/item/" + imagesArray.get(0) + ".jpg";
      }

      return null;

   }

}
