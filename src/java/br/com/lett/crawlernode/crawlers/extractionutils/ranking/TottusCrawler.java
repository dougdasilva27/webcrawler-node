package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

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


public class TottusCrawler extends CrawlerRankingKeywords {

   protected String homePage;

   public TottusCrawler(Session session) {
      super(session);
   }


   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 15;
      this.log("Página " + this.currentPage);


      String url = "https://" + homePage + "/buscar?q=" + this.keywordWithoutAccents.replace(" ", "%20") + "&page=" + this.currentPage;

      this.currentDoc = fetchDocument(url);

      JSONObject jsonInfo = CrawlerUtils.selectJsonFromHtml(this.currentDoc, "#__NEXT_DATA__", null, null, true, false);
      JSONObject props = jsonInfo.optJSONObject("props");
      JSONObject pageProps = props.optJSONObject("pageProps");
      JSONObject products = pageProps.optJSONObject("products");
      JSONArray results = products.optJSONArray("results");


      if (!results.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Object e : results) {

            JSONObject skuInfo = (JSONObject) e;
            String internalId = skuInfo.optString("sku");
            String productUrl = CrawlerUtils.completeUrl(skuInfo.optString("key"), "https://", homePage) + "/p/";
            String name = scrapName(skuInfo);
            String imgUrl = scrapImg(skuInfo);
            Integer price = scrapPrice(skuInfo);
            boolean isAvailable = scrapAvailable(skuInfo);
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
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private String scrapName(JSONObject prod) {
      String name = prod.optString("name");
      String marca = JSONUtils.getValueRecursive(prod, "attributes.marca", String.class);
      String format = JSONUtils.getValueRecursive(prod, "attributes.formato", String.class);
      StringBuilder fullName = new StringBuilder();
      if (name != null && !name.isEmpty()) {
         fullName.append(name);
      }
      if (marca != null && !marca.isEmpty()) {
         fullName.append(" ");
         fullName.append(marca);
      }
      if (format != null && !format.isEmpty()) {
         fullName.append(" ");
         fullName.append(format);
      }
      return fullName.toString();

   }

   private String scrapImg(JSONObject prod) {
      return prod.optJSONArray("images").optString(0);
   }

   private Integer scrapPrice(JSONObject prod) {
      Integer price = JSONUtils.getValueRecursive(prod, "prices.cmrPrice", Integer.class, 0);

      if (price == 0) {
         price = JSONUtils.getValueRecursive(prod, "prices.currentPrice", Integer.class);

      }

      return price;
   }

   private boolean scrapAvailable(JSONObject prod) {
      String state = JSONUtils.getValueRecursive(prod, "attributes.estado", String.class);

      return state != null && state.equals("activo");
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".searchQuery span", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }

}
