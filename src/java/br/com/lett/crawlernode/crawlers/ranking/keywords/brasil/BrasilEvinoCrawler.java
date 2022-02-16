package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilEvinoCrawler extends CrawlerRankingKeywords {
   public String HOME_PAGE = "https://www.evino.com.br";

   public BrasilEvinoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 51;

      String url = HOME_PAGE + "/api/product-list/slug/?q=" + this.keywordEncoded + "&perPage=51&page=" + this.currentPage;
      JSONObject productsJson = fetchJSONObject(url);
      JSONObject data = productsJson.has("data") ? productsJson.getJSONObject("data") : new JSONObject();
      JSONArray products = data.has("products") ? data.getJSONArray("products") : new JSONArray();

      this.log("Página " + this.currentPage);

      this.log("Link onde são feitos os crawlers: " + url);

      if (productsJson.length() != 0) {

         if (this.totalProducts == 0) {
            setTotalProducts(data);
         }

         for (Object object : products) {
            JSONObject product = (JSONObject) object;
            String productUrl = CrawlerUtils.completeUrl(product.optString("url"), "https", "www.evino.com.br");
            String internalId = product.optString("sku");
            String imageUrl = crawlImage(product);
            String name = product.optString("name");
            int price = crawlPrice(product);
            boolean isAvailable = product.optInt("quantity") > 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(null)
               .setImageUrl(imageUrl)
               .setName(name)
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

   private String crawlImage(JSONObject product) {
      JSONObject images = product.optJSONObject("images");
      if (images != null) {
         if (images.has("extralarge")) {
            return images.optString("extralarge");
         } else {
            return images.optString("thumbnail");
         }
      }
      return null;
   }

   private int crawlPrice(JSONObject product) {
      JSONObject prices = product.optJSONObject("prices");
      if (prices != null) {
         return prices.optInt("sale", 0);
      }
      return 0;
   }

   private void setTotalProducts(JSONObject data) {
      JSONObject meta = data.has("meta") ? data.getJSONObject("meta") : new JSONObject();

      if (meta.has("total")) {
         this.totalProducts = meta.getInt("total");
      }
   }

}
