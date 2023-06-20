package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

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

abstract public class TrayCommerceCrawler extends CrawlerRankingKeywords {

   protected final String homePage = setHomePage();
   protected final String storeId = setStoreId();

   protected abstract String setStoreId();

   public TrayCommerceCrawler(Session session) {
      super(session);
   }

   protected abstract String setHomePage();

   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 20;
      this.log("Página " + this.currentPage);
      String url = homePage + "loja/busca.php?loja=" + storeId + "&palavra_busca=" + this.keywordEncoded + "&pg=" + this.currentPage;
      this.currentDoc = fetchDocument(url);

      JSONObject search = CrawlerUtils.selectJsonFromHtml(this.currentDoc, "script", "dataLayer = [", "]", false, true);

      if (search.has("listProducts") && search.getJSONArray("listProducts").length() > 0) {
         JSONArray products = search.getJSONArray("listProducts");
         if (this.totalProducts == 0) {
            setTotalProducts(search);
         }
         for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.getJSONObject(i);
            String productUrl = product.optString("urlProduct");
            String internalId = product.optString("idProduct");
            String productName = product.optString("nameProduct");
            String imageUrl = product.optString("urlImage");
            Integer price = CommonMethods.stringPriceToIntegerPrice(product.optString("price", ""), ',', 0);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalId)
               .setName(productName)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
               .build();

            saveDataProduct(productRanking);
            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
      this.log("Finalizando Crawler de produtos da página: " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   protected void setTotalProducts(JSONObject search) {
      this.totalProducts = JSONUtils.getIntegerValueFromJSON(search, "siteSearchResults", 0);
      this.log("Total da busca: " + this.totalProducts);
   }
}
