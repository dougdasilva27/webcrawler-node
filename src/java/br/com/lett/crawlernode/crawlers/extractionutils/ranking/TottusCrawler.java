package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
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
import java.util.List;


public class TottusCrawler extends CrawlerRankingKeywords {

   protected String homePage;
   private String urlWithCode;

   public TottusCrawler(Session session) {
      super(session);
   }


   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 48;
      this.log("Página " + this.currentPage);

      if (urlWithCode == null) {
         urlWithCode = getUrl();
      }

      String url = urlWithCode + "&page=" + this.currentPage;

      this.currentDoc = fetchDocument(url);

      JSONObject jsonInfo = CrawlerUtils.selectJsonFromHtml(this.currentDoc, "#__NEXT_DATA__", null, null, false, false);
      JSONArray results = JSONUtils.getValueRecursive(jsonInfo, "props.pageProps.products.results", JSONArray.class);

      if (results != null && !results.isEmpty()) {
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

   private JSONObject fetchJsonFromApi(String url) {
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setProxyservice(List.of(ProxyCollection.NETNUT_RESIDENTIAL_BR, ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY))
         .build();

      Response response = this.dataFetcher.get(session, request);
      return CrawlerUtils.stringToJson(response.getBody());


   }

   private String getUrl() {

      String url = "https://www.tottus.com.pe/api/product-search?q=" + keywordEncoded + "&perPage=48&channel=912_RegularDelivery12&categoryId=";
      JSONObject json = fetchJsonFromApi(url);
      if (json != null && json.has("redirect")) {
         urlWithCode = json.optString("redirect");
      } else {
         urlWithCode = "https://www.tottus.com.pe/buscar?q=" + this.keywordEncoded.replace(" ", "%20");
      }

      return urlWithCode;

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
      Double priceDouble = JSONUtils.getValueRecursive(prod, "prices.cmrPrice", Double.class, 0.0);
      if (priceDouble == 0.0) {
         priceDouble = JSONUtils.getValueRecursive(prod, "prices.currentPrice", Double.class, 0.0);
         if (priceDouble == 0.0) {
            return JSONUtils.getValueRecursive(prod, "prices.currentPrice", Integer.class, 0);
         }
      }
      Integer price = (int) Math.round(100 * priceDouble);
      return price;
   }

   private boolean scrapAvailable(JSONObject prod) {
      String state = JSONUtils.getValueRecursive(prod, "attributes.estado", String.class);

      return state != null && state.equals("activo");
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".Facets .facet-total-products", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }

}
