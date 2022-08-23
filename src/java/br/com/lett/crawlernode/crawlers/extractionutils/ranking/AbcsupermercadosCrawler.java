package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AbcsupermercadosCrawler extends CrawlerRankingKeywords {

   public AbcsupermercadosCrawler(Session session) {
      super(session);
   }

   private final String idLoja = getIdLoja();

   protected String getIdLoja() {
      return session.getOptions().optString("id_loja");
   }

   @Override
   protected JSONObject fetchJSONObject(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("authorization", "Basic YjQ5Y2ZlYTEtMTI4OS00YmNmLWE3M2UtMDkxMTVhZjQ4ZWNlOjY4MDZhZGY4Y2QyNGZmZGU2MGFhNGUwY2FmZDdmM2Qx");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR
         ))
         .build();

      Response response = this.dataFetcher.get(session, request);
      return JSONUtils.stringToJson(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {

      this.pageSize = 30;
      this.log("Página " + this.currentPage);
      String url = "https://apiofertas.superabc.com.br/api/app/v3/selecionarprodutosnovo/?loja=" + idLoja
         + "&descricao=" + this.keywordEncoded + "&page=" + this.currentPage + "&pagesize=30";
      JSONObject jsonObject = fetchJSONObject(url);

      this.log("Link onde são feitos os crawlers: " + url);

      JSONArray products = jsonObject.optJSONArray("Produtos");

      if (products != null && products.length() > 0) {
         for (Object product : products) {
            if (product instanceof JSONObject) {
               JSONObject productJson = (JSONObject) product;
               String internalId = productJson.optString("Codigo");
               String internalPid = internalId;
               String name = productJson.optString("Descricao");
               String productUrl = scrapProductUrl(productJson);
               String image = productJson.optString("Urls/0");
               Integer price = (int) Math.round((productJson.optDouble("PrecoVenda") * 100));
               boolean available = JSONUtils.getIntegerValueFromJSON(productJson, "EstoqueDisponivel", 0) > 0;

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setUrl(productUrl)
                  .setImageUrl(image)
                  .setAvailability(available)
                  .setPriceInCents(price)
                  .build();

               saveDataProduct(productRanking);
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultados!");
      }

   }

   private String scrapProductUrl(JSONObject productJson) {
      String partialUrl = productJson.optString("UrlSite");
      return "https://superabc.com.br/" + partialUrl + "/p";
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }
}
