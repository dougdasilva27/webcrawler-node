package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.MathUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SaopauloPanvelCrawler extends CrawlerRankingKeywords {

   public SaopauloPanvelCrawler(Session session) {
      super(session);
      fetchMode = FetchMode.FETCHER;
   }


   protected String fetchDocument() {
      String payload = "{\"term\":\""+ location +"\",\"itemsPerPage\":16,\"currentPage\":"+currentPage+",\"assortment\":\"\",\"categoryId\":null,\"filters\":[],\"searchType\":\"term\"}";
       cookies.add(new BasicClientCookie("appName", "home"));
      Map<String, String> headers = new HashMap<>();
      headers.put("client-ip", "1");
      headers.put(HttpHeaders.CONTENT_TYPE,"application/json");
      headers.put("user-id", "8601417");
      headers.put("app-token", "ZYkPuDaVJEiD");
      Request request = Request.RequestBuilder.create()
         .setCookies(cookies)
         .setUrl("https://www.panvel.com/api/v2/search/")
         .setPayload(payload)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(ProxyCollection.BUY, ProxyCollection.NETNUT_RESIDENTIAL_BR))
         .build();
      Response response = dataFetcher.post(session, request);
      return response.getBody();
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 16;
      this.log("Página " + this.currentPage);

      String url = "https://www.panvel.com/panvel/buscarProduto.do?termoPesquisa=" + keywordEncoded + "&paginaAtual=" + currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      String body = fetchDocument();
      JSONObject storeJson = JSONUtils.stringToJson(body);

      JSONArray products = JSONUtils.getValueRecursive(storeJson , "items", JSONArray.class);

      if (!products.isEmpty()) {

            if (this.totalProducts == 0) {
               this.totalProducts = JSONUtils.getValueRecursive(storeJson, "totalItems", Integer.class);
            }
         for (Object e : products) {
            JSONObject productJson =(JSONObject) e;
            String urlProduct = "https://www.panvel.com" + productJson.optString("link");
            String internalId = productJson.optString("panvelCode");
            String name = productJson.optString("name");
            String image = productJson.optString("image");
            Double price = productJson.optJSONObject("discount").optDouble("dealPrice");
            Integer priceInCents = price != null ? MathUtils.parseInt(price * 100) : 0;
            boolean isAvailable = priceInCents > 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(urlProduct)
               .setInternalId(internalId)
               .setName(name)
               .setImageUrl(image)
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
   @Override
   protected boolean hasNextPage() {
      return (arrayProducts.size() % pageSize - currentPage) < 0;
   }
}
