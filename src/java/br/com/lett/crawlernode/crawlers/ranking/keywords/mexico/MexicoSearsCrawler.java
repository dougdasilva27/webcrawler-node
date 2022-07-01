package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
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
import br.com.lett.crawlernode.util.MathUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MexicoSearsCrawler extends CrawlerRankingKeywords {
   public MexicoSearsCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.JSOUP;
   }

   private JSONObject fetchSearch(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("Host", "seapi.sears.com.mx");
      headers.put("Accept", "*/*");


      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_MX
         ))
         .build();

      Response response = CrawlerUtils.retryRequest(request, session, this.dataFetcher, true);

      return CrawlerUtils.stringToJson(response.getBody());
   }

   @Override
   public void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 100;
      String url = "https://seapi.sears.com.mx/anteater/search?cliente=sears_v2&proxystylesheet=xml2json&oe=UTF-8&getfields=*&sort=&start=" + ((this.currentPage - 1) * this.pageSize) + "&num=" + this.pageSize + "&q=" + this.keywordEncoded + "&requiredfields=&ds=marcas:attribute_marca:0:0:100:0:1.sale_precio:sale_price:1:1:1000.ranking:review:0:0:8:0:1.full:fulfillment:0:0:8:0:1.free:shipping_price:0:0:8:0:1.discount:discount:0:0:1000:0:1&do=breadcrumbs:breadcrumbs:id,name,padre:100:1:2:1&requiredobjects=&client=pwa";

      JSONObject searchJson = fetchSearch(url);

      JSONArray products = JSONUtils.getValueRecursive(searchJson, "GSP.RES.R", ".", JSONArray.class, new JSONArray());

      if (this.totalProducts == 0) {
         String totalProductsStr = JSONUtils.getValueRecursive(searchJson, "GSP.RES.M", String.class, "");
         Integer totalProducts = MathUtils.parseInt(totalProductsStr);
         this.totalProducts = !totalProductsStr.equals("") && totalProducts != null ? totalProducts : 0;
         this.log("Total: " + this.totalProducts);
      }

      if (products.length() > 0) {
         for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.optJSONObject(i);
            String productId = getStringValueByKey(product, "id");
            String productPid = getStringValueByKey(product, "sku");
            String name = product.optString("T");
            String imgUrl = getStringValueByKey(product, "link");
            String priceStr = getStringValueByKey(product, "price");
            Integer priceInCents = priceStr != null ? MathUtils.parseInt(priceStr) * 100 : null;
            boolean isAvailable = priceInCents != null;
            String titleSeo = getStringValueByKey(product, "title_seo");
            String productUrl = "https://www.sears.com.mx/producto/" + productId + "/" + (titleSeo != null ? titleSeo : "");

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

   private String getStringValueByKey(JSONObject product, String key) {
      JSONArray data = product.optJSONArray("MT");

      for (int i = 0; i < data.length(); i++) {
         JSONObject item = data.optJSONObject(i);
         if (item != null && item.optString("N").equals(key)) {
            return item.optString("V");
         }
      }

      return null;
   }
}
