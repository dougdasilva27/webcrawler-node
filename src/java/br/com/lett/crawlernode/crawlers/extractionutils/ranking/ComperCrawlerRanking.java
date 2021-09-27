package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import java.util.HashMap;
import java.util.Map;

import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.models.RankingProducts;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
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
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public abstract class ComperCrawlerRanking extends CrawlerRankingKeywords {

   protected final String storeId = getStoreId();
   protected final String storeUf = getStoreUf();

   public ComperCrawlerRanking(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   protected abstract String getStoreId();

   protected abstract String getStoreUf();

   @Override
   protected void processBeforeFetch() {
      BasicClientCookie cookie = new BasicClientCookie("VTEXSC", "sc=" + getStoreId());
      cookie.setDomain("www.comper.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   protected JSONObject fetchApi() {
      Map<String, String> headers = new HashMap<>();

      String url = "https://api.linximpulse.com/engage/search/v3/search?apikey=comper&terms=" +
         this.keywordEncoded +
         "&resultsPerPage=32&salesChannel=2&sortBy=relevance&showOnlyAvailable=false&allowRedirect=true&page=" +
         this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);

      headers.put("origin", "https://www.comper.com.br");

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setCookies(cookies)
         .build();
      Response response = this.dataFetcher.get(session, request);
      return JSONUtils.stringToJson(response.getBody());
   }

   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 32;

      this.log("Página " + this.currentPage);

      JSONObject json = fetchApi();

      JSONArray products = json.optJSONArray("products");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            this.totalProducts = json.optInt("size");
         }

         for (Object o : products) {
            JSONObject product = (JSONObject) o;

            String productUrl = "https://www.comper.com.br" + product.optString("url");
            String internalId = product.optString("id");
            String imageUrl = "https://comper.vteximg.com.br" + JSONUtils.getValueRecursive(product, "images.1000x1000", String.class);
            String name = product.optString("name");
            String priceStr = product.optString("price");
            int price = priceStr != null ? MathUtils.parseInt(priceStr) : 0;
            boolean isAvailable = price != 0;

            RankingProducts objProducts = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setImageUrl(imageUrl)
               .setName(name)
               .setPriceInCents(price)
               .setInternalId(internalId)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(objProducts);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

}
