package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
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
import java.util.Map;

public class MundodanoneCrawler extends CrawlerRankingKeywords {

   public MundodanoneCrawler(Session session) {
      super(session);
   }


   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {

      this.log("Página " + this.currentPage);

      String url = "https://service.mundodanone.com.br/graphql?query=query+ProductSearch%28%24currentPage%3AInt%3D1%24inputText%3AString%21%24pageSize%3AInt%3D6%24filters%3A" +
         "ProductAttributeFilterInput%21%24sort%3AProductAttributeSortInput%29%7Bproducts%28currentPage%3A%24currentPage+pageSize%3A%24pageSize+search%3A%24inputText+filter%3A%" +
         "24filters+sort%3A%24sort%29%7Bitems%7Bid+name+tags%7Bimage+name+__typename%7Dsmall_image%7Burl+__typename%7Durl_key+url_suffix+price_range%7Bminimum_price%7Bregular_price" +
         "%7Bvalue+currency+__typename%7Dfinal_price%7Bvalue+currency+__typename%7Ddiscount%7Bpercent_off+__typename%7D__typename%7D__typename%7Dprice_tiers%7Bdiscount%7Bamount_off+" +
         "percent_off+__typename%7Dfinal_price%7Bcurrency+value+__typename%7Dquantity+__typename%7Dshort_description%7Bhtml+__typename%7Drating_summary+review_count+...on+ConfigurableProduct" +
         "%7Bvariants%7Bproduct%7Bcustom_flavor+__typename%7D__typename%7D__typename%7D__typename%7Dpage_info%7Btotal_pages+__typename%7Dtotal_count+__typename%7D%7D&operationName=ProductSearch" +
         "&variables=%7B%22currentPage%22%3A" + this.currentPage +"%2C%22pageSize%22%3A12%2C%22filters%22%3A%7B%7D%2C%22inputText%22%3A%22" + this.keywordEncoded.replace(" ", "+") + "%22%2C%22sort%22%3A%7B%22relevance%22%3A%22DESC%22%7D%7D";

      JSONObject json = fetchJSONObject(url);
      JSONObject resultsList = JSONUtils.getValueRecursive(json, "data.products", JSONObject.class);
      JSONArray productsArray = JSONUtils.getValueRecursive(resultsList, "items", JSONArray.class);

      if (productsArray != null && !productsArray.isEmpty()) {
         setTotalProducts(resultsList);
         for (Object o : productsArray) {
            JSONObject product = (JSONObject) o;
               String internalPid = product.optString("id");
               String productUrl = CrawlerUtils.completeUrl(product.optString("url_key") + ".html?page=1", "https", "www.mundodanone.com.br");
               String name = product.optString("name");
               String imageUrl = JSONUtils.getValueRecursive(product, "small_image.url", String.class);
               int price = crawlPrice(product);
               boolean isAvailable = price != 0;

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setPriceInCents(price)
                  .setAvailability(isAvailable)
                  .setImageUrl(imageUrl)
                  .build();

               saveDataProduct(productRanking);

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

   private int crawlPrice(JSONObject product){
      JSONObject priceInfo = product.optJSONObject("price_range");
      Double spotlightPrice = priceInfo != null ? JSONUtils.getValueRecursive(priceInfo, "minimum_price.regular_price.value", Double.class) : null;

      return CommonMethods.doublePriceToIntegerPrice(spotlightPrice, 0);
   }

   protected void setTotalProducts(JSONObject resultsList) {
         this.totalProducts = resultsList.optInt("total_count", 0);
         this.log("Total da busca: " + this.totalProducts);

   }

}
