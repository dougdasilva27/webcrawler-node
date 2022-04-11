package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.MathUtils;
import org.json.JSONArray;
import org.json.JSONObject;


public class ArgentinaHiperlibertadCrawler extends CrawlerRankingKeywords {

   public ArgentinaHiperlibertadCrawler(Session session) {
      super(session);
      this.pageSize = 24;
   }

   private JSONArray getProducts() {
      int start = (currentPage - 1) * pageSize;
      int end = currentPage * pageSize - 1;
      String url = "https://www.hiperlibertad.com.ar/api/catalog_system/pub/products/search/busca?O=OrderByTopSaleDESC" +
         "&_from=" + start +
         "&_to=" + end +
         "&ft=" + keywordWithoutAccents.replace(" ", "%20") +
         "&sc=1";
      Response response = dataFetcher.get(session, Request.RequestBuilder.create().setUrl(url).build());
      return JSONUtils.stringToJsonArray(response.getBody());
   }

   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {
      JSONArray products = getProducts();
      for (Object o : products) {
         if (o instanceof JSONObject) {
            JSONObject product = (JSONObject) o;
            String internalPid = product.optString("productId");
            String productUrl = product.optString("link");
            String productName = product.optString("productName");
            String productImage = (String) product.optQuery("/items/0/images/0/imageUrl");
            Boolean productIsAvailable = (Boolean) product.optQuery("/items/0/sellers/0/commertialOffer/IsAvailable");
            Double productPrice = (Double) product.optQuery("/items/0/sellers/0/commertialOffer/Price");
            Integer productPriceInCents = null;
            if (productIsAvailable && productPrice != null) {
               productPriceInCents = MathUtils.parseInt( productPrice * 100);
            }
            
            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
               .setName(productName)
               .setPriceInCents(productPriceInCents)
               .setAvailability(productIsAvailable)
               .setImageUrl(productImage)
               .build();

            saveDataProduct(productRanking);
         }
      }
   }

   public boolean hasNextPage() {
      return (arrayProducts.size() % pageSize - currentPage) < 0;
   }
}
