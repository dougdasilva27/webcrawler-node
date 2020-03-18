package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;

public class SaopauloExtramarketplaceCrawler extends CrawlerRankingKeywords {

   public SaopauloExtramarketplaceCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   protected String mainSellerNameLower;
   protected String mainSellerNameLower2;
   protected String marketHost = "www.extra.com.br";
   protected static final String PROTOCOL = "https";

   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);

      String url = "https://www.extra.com.br/busca?q=" + this.keywordEncoded + "&page=" + this.currentPage + "&ajaxSearch=1";
      this.log("Link onde são feitos os crawlers: " + url);

      JSONObject searchApi = JSONUtils.stringToJson(fetchPage(url));

      JSONObject jsonProducts = searchApi.has("productsInfo") ? searchApi.getJSONObject("productsInfo") : new JSONObject();
      JSONArray products = jsonProducts.has("products") ? jsonProducts.getJSONArray("products") : new JSONArray();


      this.pageSize = 24;

      if (products.length() > 0) {

         if (this.totalProducts == 0) {
            setTotalProducts(searchApi);
         }

         for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.getJSONObject(i);

            String internalIdString = crawlInternalId(product);
            String internalPid = crawlInternalPid(product);
            String productUrl = crawlProductUrl(product);


            saveDataProduct(null, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalIdString + " - InternalPid: " +
                  internalPid + " - Url: " + productUrl);

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

   private String crawlInternalId(JSONObject product) {
      String internalId = null;

      JSONArray skus = product.has("skus") ? product.getJSONArray("skus") : new JSONArray();

      for (int i = 0; i < skus.length(); i++) {

         JSONObject sku = skus.getJSONObject(i);

         if (sku.has("sku")) {
            internalId = sku.get("sku").toString();

         }

      }
      return internalId;
   }

   private String crawlInternalPid(JSONObject product) {
      String internalPid = null;

      if (product.has("originalId")) {
         internalPid = product.get("originalId").toString();
      }

      return internalPid;
   }

   private String crawlProductUrl(JSONObject product) {
      String productUrl = null;

      if (product.has("productUrl")) {
         productUrl = CrawlerUtils.completeUrl(product.get("productUrl").toString(), "https:", "www.extra.com.br");
      }

      return productUrl;
   }

   protected void setTotalProducts(JSONObject searchApi) {

      JSONObject totalProducts = searchApi.has("totalProducts") ? searchApi.getJSONObject("totalProducts") : new JSONObject();

      this.totalProducts = JSONUtils.getIntegerValueFromJSON(totalProducts, "totalResults", 0);
      this.log("Total da busca:" + this.totalProducts);

   }

   protected String fetchPage(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
      headers.put("Accept-Enconding", "");
      headers.put("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
      headers.put("Cache-Control", "no-cache");
      headers.put("Connection", "keep-alive");
      headers.put("Host", this.marketHost);
      headers.put("Referer", PROTOCOL + "://" + this.marketHost + "/");
      headers.put("Upgrade-Insecure-Requests", "1");
      headers.put("User-Agent", FetchUtilities.randUserAgent());

      Request request = RequestBuilder.create()
            .setUrl(url)
            .setCookies(cookies)
            .setHeaders(headers)
            .setProxyservice(
                  Arrays.asList(
                        ProxyCollection.INFATICA_RESIDENTIAL_BR,
                        ProxyCollection.BUY,
                        ProxyCollection.STORM_RESIDENTIAL_US
                  )
            ).build();

      return this.dataFetcher.get(session, request).getBody();
   }
}
