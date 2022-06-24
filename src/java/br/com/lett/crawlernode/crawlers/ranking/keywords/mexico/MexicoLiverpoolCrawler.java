package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.MathUtils;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MexicoLiverpoolCrawler extends CrawlerRankingKeywords {
   public MexicoLiverpoolCrawler(Session session) {
      super(session);
   }

   private Document fetch(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("Host", "www.liverpool.com.mx");
      headers.put("Accept", "*/*");
      headers.put("Accept-Encoding", "gzip, deflate");
      headers.put("Accept-Language", "en-US,en;q=0.5");


      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_MX,
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY
         ))
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session, "get");

      return Jsoup.parse(response.getBody());
   }

   @Override
   public void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 56;
      String url = "https://www.liverpool.com.mx/tienda/page-" + this.currentPage + "?s=" + this.keywordEncoded;

      this.currentDoc = fetch(url);

      JSONObject pageJson = CrawlerUtils.selectJsonFromHtml(this.currentDoc, "#__NEXT_DATA__", null, null, false, false);
      JSONArray products = JSONUtils.getValueRecursive(pageJson, "query.data.mainContent.records", JSONArray.class, new JSONArray());

      if (this.totalProducts == 0) {
         this.totalProducts = JSONUtils.getValueRecursive(pageJson, "query.data.mainContent.totalRecordCount", Integer.class, 0);
         this.log("Total: " + this.totalProducts);
      }

      if (products.length() > 0) {
         for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.optJSONObject(i).optJSONObject("allMeta");
            String productId = product.optString("id");
            String productPid = productId;
            String name = product.optString("title");
            String imgUrl = JSONUtils.getValueRecursive(product, "productImages.0.largeImage", String.class, null);
            double price = product.optDouble("minimumListPrice");
            Integer priceInCents = price != 0.0 ? MathUtils.parseInt(price * 100) : null;
            boolean isAvailable = priceInCents != null;
            String productUrl = scrapUrl(name, productId);

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(productId)
               .setInternalPid(productPid)
               .setImageUrl(imgUrl)
               .setName(name)
               .setPriceInCents(priceInCents)
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

   private String scrapUrl(String name, String productId) {
      if (name != null && productId != null) {
         String baseUrl = "https://www.liverpool.com.mx/tienda/pdp/";
         String nameFormatted = name.replace(' ', '-');

         return baseUrl + nameFormatted + "/" + productId + "?skuId=" + productId;
      }

      return null;
   }
}
