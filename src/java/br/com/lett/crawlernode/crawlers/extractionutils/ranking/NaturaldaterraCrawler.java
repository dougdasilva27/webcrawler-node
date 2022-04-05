package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
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
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class NaturaldaterraCrawler extends CrawlerRankingKeywords {

   public NaturaldaterraCrawler(Session session) {
      super(session);
   }

   protected JSONObject fetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put("store", "natural_da_terra");

      JSONObject paramsUrl = new JSONObject();
      JSONObject sort = new JSONObject();
      sort.put("relevance", "DESC");
      paramsUrl.put("currentPage", this.currentPage);
      paramsUrl.put("pageSize", this.pageSize);
      paramsUrl.put("filters", new JSONObject());
      paramsUrl.put("inputText", this.keywordEncoded);
      paramsUrl.put("sort", sort);
      paramsUrl.put("sourceCode", session.getOptions().optString("code"));

      String paramsEncoded = null;
      try {
         paramsEncoded = URLEncoder.encode(paramsUrl.toString(), "UTF-8");
      } catch (UnsupportedEncodingException e) {
         e.printStackTrace();
      }

      String url = "https://naturaldaterra.com.br/graphql?query=query+ProductSearch%28%24currentPage%3AInt%3D1%24inputText%3AString%21%24pageSize%3AInt%3D6%24filters%3AProductAttributeFilterInput%21%24sort%3AProductAttributeSortInput%24sourceCode%3AString%29%7Bproducts%28currentPage%3A%24currentPage+pageSize%3A%24pageSize+search%3A%24inputText+filter%3A%24filters+sort%3A%24sort+sourceCode%3A%24sourceCode%29%7Bitems%7Bid+sku+name+product_title_description+medium_product_weight+...on+SimpleProduct%7Bpromotion_label+stock_status+stock_quantity+unity_price_description+total_unity_price+total_unity_description+__typename%7Dprice_range%7Bmaximum_price%7Bregular_price%7Bcurrency+value+__typename%7Dfinal_price%7Bcurrency+value+__typename%7Ddiscount%7Bamount_off+percent_off+__typename%7D__typename%7D__typename%7Dprice_tiers%7Bdiscount%7Bamount_off+percent_off+__typename%7Dfinal_price%7Bcurrency+value+__typename%7Dquantity+__typename%7Dsmall_image%7Burl+__typename%7Durl_key+url_suffix+categories%7Bname+__typename%7D__typename%7Dpage_info%7Btotal_pages+__typename%7Dtotal_count+__typename%7D%7D&operationName=ProductSearch&variables=" +
         paramsEncoded;
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();
      String content = new JsoupDataFetcher().get(session, request).getBody();

      return CrawlerUtils.stringToJson(content);
   }


   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 25;
      this.log("Página " + this.currentPage);

      JSONObject data = fetch();
      JSONArray items = JSONUtils.getValueRecursive(data, "data.products.items", JSONArray.class);
      if (!items.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts(data);
         }

         for (Object o : items) {
            if (o instanceof JSONObject) {
               JSONObject product = (JSONObject) o;

               String internalId = product.optString("id");
               String internalPid = product.optString("sku");
               String productUrl = CrawlerUtils.completeUrl(product.optString("url_key") + product.optString("url_suffix"), "https", "naturaldaterra.com.br");
               String name = product.optString("name");
               String imageUrl = JSONUtils.getValueRecursive(product, "small_image.url", String.class);
               int price = getPrice(product);
               boolean isAvailable = price != 0;

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

   private int getPrice(JSONObject product) {
      int priceCents = 0;
      Double price = JSONUtils.getValueRecursive(product, "price_tiers.0.final_price.value", Double.class);
      String text = price.toString().replaceAll("[^0-9]", "");

      if (!text.isEmpty()) {
         priceCents = Integer.parseInt(text);
      }
      return priceCents;

   }

   protected void setTotalProducts(JSONObject data) {
      this.totalProducts = JSONUtils.getValueRecursive(data, "data.products.total_count", Integer.class);
      this.log("Total da busca: " + this.totalProducts);
   }

}
