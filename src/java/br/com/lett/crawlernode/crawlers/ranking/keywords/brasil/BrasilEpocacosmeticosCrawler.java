package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilEpocacosmeticosCrawler extends CrawlerRankingKeywords {

   public BrasilEpocacosmeticosCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.APACHE;
   }

   @Override
   public void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 24;

      this.log("Página " + this.currentPage);

      JSONObject search = crawlSearchApi();

      if (search.has("docs") && search.getJSONArray("docs").length() > 0) {
         JSONArray products = search.getJSONArray("docs");

         if (this.totalProducts == 0) {
            setTotalProducts(search);
         }

         for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.getJSONObject(i);

            String productUrl = crawlProductUrl(product);
            String internalPid = crawlInternalPid(product);

            String name = product.optString("name");
            String imgUrl = product.optString("imageId");
            Integer price = product.optInt("priceCents");
            boolean  isAvailable  = crawlStock(product);
            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
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

   protected void setTotalProducts(JSONObject search) {
      if (search.has("numFound") && search.get("numFound") instanceof Integer) {
         this.totalProducts = search.getInt("numFound");
         this.log("Total da busca: " + this.totalProducts);
      }
   }

   private Boolean crawlStock(JSONObject product) {
      String stock = product.optString("estoque");
      if(stock.equals("BAIXO") || stock.equals("MEDIO") || stock.equals("ALTO")){
         return true;
      }
      return false;
   }

   private String crawlInternalPid(JSONObject product) {
      String internalPid = null;

      if (product.has("id")) {
         internalPid = product.get("id").toString();
      }

      return internalPid;
   }

   private String crawlProductUrl(JSONObject product) {
      String urlProduct = null;

      if (product.has("linkId")) {
         urlProduct = product.getString("linkId");
      }

      return urlProduct;
   }

   private JSONObject crawlSearchApi() {
      JSONObject searchApi = new JSONObject();
      String url = "https://recs.richrelevance.com/rrserver/api/find/v1/c85912f892c73e30?lang=pt" + "&query=" + this.keywordEncoded
            + "&log=true&userId=&placement=search_page.find" + "&start=" + this.arrayProducts.size() + "&rows=32";
      this.log("Link onde são feitos os crawlers: " + url);

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).mustSendContentEncoding(false).build();
      JSONObject json = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

      if (json.has("placements")) {
         JSONArray placements = json.getJSONArray("placements");

         if (placements.length() > 0) {
            searchApi = placements.getJSONObject(0);
         }
      }

      return searchApi;
   }
}
