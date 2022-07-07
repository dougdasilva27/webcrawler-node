package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class MexicoWalmartCrawler extends CrawlerRankingKeywords {

   public MexicoWalmartCrawler(Session session) {
      super(session);
   }

   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {
      // número de produtos por página do market
      this.pageSize = 20;

      this.log("Página " + this.currentPage);
      String url = "https://www.walmart.com.mx/api/page/search/?Ntt=" + this.keywordEncoded + "&Nrpp=24&No=" + this.arrayProducts.size();

      this.log("Link onde são feitos os crawlers: " + url);

      JSONObject search = fetchJSONApi(url);

      if (search.has("records") && search.getJSONArray("records").length() > 0) {
         JSONArray products = search.getJSONArray("records");

         if (this.totalProducts == 0) {
            setTotalProducts(search);
         }

         for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.getJSONObject(i);

            if (product.has("attributes")) {
               JSONObject attributes = product.getJSONObject("attributes");
               String productUrl = crawlProductUrl(attributes);
               String internalId = crawlInternalId(attributes);
               String name = attributes.optString("skuDisplayName").replace("[", "").replace("]", "").replace("\"", "");
               String imageUrl = "https://www.walmart.com.mx" + attributes.optString("smallImage").replace("[", "").replace("]", "").replace("\"", "");
               Integer price = crawlPrice(attributes, internalId);
               boolean isAvailable = price != null;
               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setName(name)
                  .setPriceInCents(price)
                  .setAvailability(isAvailable)
                  .setImageUrl(imageUrl)
                  .build();

               saveDataProduct(productRanking);

            }

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

   private Integer crawlPrice(JSONObject attributes, String internalId) {
      Integer price;

      Map<String, String> headers = new HashMap<>();
      headers.put("accept-encoding", "");
      headers.put("accept-language", "");
      String url = "https://www.walmart.com.mx/api/rest/model/atg/commerce/catalog/ProductCatalogActor/getProduct?id=" + internalId;
      Request request = RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setCookies(cookies)
         .setFollowRedirects(false)
         .mustSendContentEncoding(false)
         .setProxyservice(Arrays.asList(ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY))
         .build();
      Response response  = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session, "get");
      JSONObject apiJson = CrawlerUtils.stringToJson(response.getBody());
      price = JSONUtils.getValueRecursive(apiJson, "product.childSKUs.0.offerList.0.priceInfo.specialPrice", Integer.class);
      if (price == null) {
         price = JSONUtils.getValueRecursive(apiJson, "product.childSKUs.0.offerList.0.priceInfo.originalPrice", Integer.class);
      }
      return price;
   }

   protected void setTotalProducts(JSONObject search) {
      if (search.has("totalNumRecs")) {
         this.totalProducts = search.getInt("totalNumRecs");
         this.log("Total da busca: " + this.totalProducts);
      }
   }

   private String crawlInternalId(JSONObject product) {
      String internalId = null;

      if (product.has("record.id")) {
         JSONArray ids = product.getJSONArray("record.id");

         if (ids.length() > 0) {
            internalId = ids.get(0).toString();
         }
      }

      return internalId;
   }

   private String crawlProductUrl(JSONObject product) {
      String productUrl = null;
      if (product.has("productSeoUrl")) {
         JSONArray urls = product.getJSONArray("productSeoUrl");

         if (urls.length() > 0) {
            productUrl = "https://www.walmart.com.mx" + urls.get(0).toString().replace("[", "").replace("]", "");
         }
      }

      return productUrl;
   }

   private JSONObject fetchJSONApi(String url) {
      JSONObject api = new JSONObject();

      Map<String, String> headers = new HashMap<>();
      headers.put("accept-encoding", "");
      headers.put("accept-language", "");

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers)
         .mustSendContentEncoding(false)
         .setProxyservice(Arrays.asList(ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY))
         .build();
      JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

      if (response.has("contents")) {
         JSONArray contents = response.getJSONArray("contents");

         if (contents.length() > 0) {
            JSONObject content = contents.getJSONObject(0);

            if (content.has("mainArea")) {
               JSONArray mainArea = content.getJSONArray("mainArea");
               if (mainArea.length() > 0) {
                  api = mainArea.getJSONObject(2);
               }
            }
         }
      }

      return api;
   }
}
