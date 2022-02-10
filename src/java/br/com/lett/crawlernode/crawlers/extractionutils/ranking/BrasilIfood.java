package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;


public class BrasilIfood extends CrawlerRankingKeywords {

   public BrasilIfood(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   protected String storeName = session.getOptions().getString("store_name");
   protected String storeId = session.getOptions().getString("store_id");
   protected String geolocation = session.getOptions().getString("geolocation");

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      String url = "https://marketplace.ifood.com.br/v2/search/catalog-items?" + geolocation + "&channel=IFOOD&term="
         + this.keywordEncoded + "&size=36&page=" + (this.currentPage - 1) + "&item_from_merchant_ids=" + storeId;
      JSONObject apiJson = fetch(url);

      JSONArray data = JSONUtils.getValueRecursive(apiJson, "items.data", JSONArray.class);
      if (data != null && !data.isEmpty()) {

         for (Object obj : data) {
            JSONObject product = (JSONObject) obj;

            if (!product.isEmpty()) {
               String internalId = product.optString("code");
               String internalPid = internalId;
               String productUrl = getUrl(product, internalId);
               String imgUrl = crawlImage(product);
               String name = product.optString("name");
               int price = JSONUtils.getPriceInCents(product, "price");
               boolean isAvailable = JSONUtils.getValueRecursive(product, "merchant.available", Boolean.class);

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setImageUrl(imgUrl)
                  .setName(name)
                  .setPriceInCents(price)
                  .setAvailability(isAvailable)
                  .build();

               saveDataProduct(productRanking);
               if (this.arrayProducts.size() == productsLimit)
                  break;

            }


            this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

   }

   private String crawlImage(JSONObject product) {
      String path = JSONUtils.getValueRecursive(product, "resources.0.fileName", String.class, "");
      return "https://static-images.ifood.com.br/image/upload/t_high/pratos/" + path;
   }

   private String getUrl(JSONObject itensObject, String internalId) {
      String slug = JSONUtils.getValueRecursive(itensObject, "merchant.slug", String.class);
      return CrawlerUtils.completeUrl(slug + "/" + storeId + "?item=" + internalId, "https", "www.ifood.com.br/delivery");
   }

   protected JSONObject fetch(String url) {
      Request request = RequestBuilder.create()
         .setUrl(url)
         .build();

      String content = this.dataFetcher.get(session, request).getBody();

      return JSONUtils.stringToJson(content);
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }
}

