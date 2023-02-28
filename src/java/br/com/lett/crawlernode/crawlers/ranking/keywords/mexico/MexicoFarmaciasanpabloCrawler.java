package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
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
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.List;

public class MexicoFarmaciasanpabloCrawler extends CrawlerRankingKeywords {
   public MexicoFarmaciasanpabloCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   @Override
   protected JSONObject fetchJSONObject(String url) {
      Request request = Request.RequestBuilder.create().setUrl(url).build();
      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(dataFetcher, new FetcherDataFetcher(), new ApacheDataFetcher()), session);
      return CrawlerUtils.stringToJson(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);

      String url = "https://api.farmaciasanpablo.com.mx/rest/v2/fsp/products/search?query=" + this.keywordEncoded.replace(" ", "%20") + "&currentPage=" + (this.currentPage - 1) + "&pageSize=" + this.pageSize + "&lang=es_MX&curr=MXN";
      JSONObject jsonInfo = fetchJSONObject(url);
      JSONArray products = jsonInfo.optJSONArray("products");

      if (products != null && !products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts(jsonInfo);
         }
         for (Object e : products) {
            JSONObject skuInfo = (JSONObject) e;
            String internalId = skuInfo.optString("code");
            String productUrl = CrawlerUtils.completeUrl(skuInfo.optString("url"), "https", "www.farmaciasanpablo.com.mx");
            String name = skuInfo.optString("name");
            String imgUrl = getImageUrl(skuInfo);
            Integer price = getPrice(skuInfo);
            boolean isAvailable = skuInfo.optBoolean("availableForPurchase");
            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalId)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
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

   protected void setTotalProducts(JSONObject jsonInfo) {
      this.totalProducts = JSONUtils.getValueRecursive(jsonInfo, "pagination.totalResults", Integer.class, 0);
      this.log("Total da busca: " + this.totalProducts);
   }

   private String getImageUrl(JSONObject object) {
      String imageUrl = null;
      JSONArray imageArray = JSONUtils.getValueRecursive(object, "images", JSONArray.class);
      if (imageArray != null && !imageArray.isEmpty()) {
         JSONObject objArray = JSONUtils.getValueRecursive(imageArray, "1", JSONObject.class);
         imageUrl = JSONUtils.getValueRecursive(objArray, "url", String.class);
      }
      return imageUrl;
   }

   private Integer getPrice(JSONObject object) {
      Integer price = null;
      JSONObject priceFormat = JSONUtils.getValueRecursive(object, "basePrice", JSONObject.class);
      if (priceFormat != null && !priceFormat.isEmpty()) {
         Double priceDouble = priceFormat.optDouble("value");
         price = CommonMethods.doublePriceToIntegerPrice(priceDouble, 0);
      }
      return price;
   }
}
