package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class BrasilShopeeCrawler extends CrawlerRankingKeywords {
   public BrasilShopeeCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      Integer currentPageUrl = (this.currentPage - 1) * 60;
      String url = "https://shopee.com.br/api/v4/search/search_items?by=relevancy&keyword=" + this.keywordEncoded + "&limit=60&match_id="+session.getOptions().optString("storeId")+"&newest=" + currentPageUrl + "&official_mall=1&order=desc&page_type=shop&scenario=PAGE_SHOP_SEARCH&version=2";
      JSONObject json = fetchJSONObject(url);
      JSONArray products = JSONUtils.getValueRecursive(json, "items", JSONArray.class);
      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            this.totalProducts = JSONUtils.getValueRecursive(json, "total_count", Integer.class);
         }
         for (Object e : products) {
            JSONObject product = (JSONObject) e;
            JSONObject itemBasic = product.getJSONObject("item_basic");
            String productUrl = scrapUrl(itemBasic);
            Long internalIdLong = itemBasic.optLong("itemid");
            String internalId = Long.toString(internalIdLong);
           String name = itemBasic.optString("name");
           String imgUrl = "https://cf.shopee.com.br/file/" + itemBasic.getString("image");
           Integer price = (Integer) itemBasic.optInt("price") /1000;
           boolean isAvailable = itemBasic.optInt("stock") != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
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
      }
   }

   private String scrapUrl(JSONObject itemBasic) {
      String url = "https://shopee.com.br/";
      String name = itemBasic.optString("name", "");
      name = name.replaceAll(" ", "-");
      url = url + name + "-i.";
      Integer shopId = itemBasic.optInt("shopid");
      Long internalIdLong = itemBasic.optLong("itemid");
      url = url + shopId + "." + internalIdLong;
      return url;
   }

   @Override
   protected JSONObject fetchJSONObject(String url) {
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .build();
      Response response = this.dataFetcher.get(session, request);
      return JSONUtils.stringToJson(response.getBody());
   }
}
