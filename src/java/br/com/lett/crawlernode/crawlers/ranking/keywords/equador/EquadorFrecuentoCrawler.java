package br.com.lett.crawlernode.crawlers.ranking.keywords.equador;

import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Locale;

import static org.bouncycastle.asn1.x509.X509ObjectIdentifiers.id;

public class EquadorFrecuentoCrawler extends CrawlerRankingKeywords {
   public EquadorFrecuentoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 20;

      String url = "https://app.frecuento.com/products-search/?limit=300&q=" + this.keywordEncoded;

      JSONObject productsResponnse = fetchJSONObject(url);
      JSONArray products = productsResponnse.getJSONArray("results");

      for (int i = 0; i < products.length(); i++) {
         if (products.get(i) instanceof JSONObject) {
            checkCurrentPage(i);
            JSONObject productJson = (JSONObject) products.get(i);

            String internalPid = productJson.getInt("id") + "";
            String name = productJson.getString("name");
            String productUrl = scrapProductUrl(name, internalPid);

            JSONArray images = productJson.getJSONArray("photos");
            String image = images.length() > 0 ? images.getString(0) : "";

            int priceInCents = CommonMethods.stringPriceToIntegerPrice(productJson.getString("amount_total"), '.', 0);
            boolean isAvailable = productJson.getInt("stock") != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setInternalId(null)
               .setInternalPid(internalPid)
               .setName(name)
               .setUrl(productUrl)
               .setImageUrl(image)
               .setAvailability(isAvailable)
               .setPriceInCents(priceInCents)
               .build();

            saveDataProduct(productRanking);
         }
      }


   }

   private String scrapProductUrl(String productName, String productId) {
      String normalizedName = CommonMethods.removeAccents(productName).replaceAll("[^0-9a-zA-Z]+", "-").toLowerCase();
      return "https://www.frecuento.com/" + normalizedName + "/" + productId;
   }

   private void checkCurrentPage(int productIndex) {
      int currentPage = productIndex / this.pageSize + 1;
      if (currentPage != this.currentPage) {
         this.currentPage = currentPage;
      }
   }

   @Override
   protected boolean hasNextPage() {
      return this.position<totalProducts;
   }
}
