package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MartinsKeywords extends CrawlerRankingKeywords {

   public MartinsKeywords(Session session) {
      super(session);
      super.fetchMode = FetchMode.JSOUP;
   }


   protected JSONArray prices;

   protected String accessToken;
   protected String password = getPassword();
   protected String login = getLogin();
   protected String cnpj = getCnpj();

   protected String getPassword() {
      return session.getOptions().optString("pass");
   }

   protected String getCnpj() {
      return session.getOptions().optString("cnpj");
   }

   protected String getLogin() {
      return session.getOptions().optString("login");
   }

   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);
      String response = fetchJson();
      JSONObject obj = JSONUtils.stringToJson(response);
      obj = JSONUtils.getValueRecursive(obj, "pageProps.fallback./api/search", JSONObject.class);

      JSONArray products = obj.getJSONArray("products");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            this.totalProducts = obj.optInt("productTotal");
         }
         fetchPrices(products);
         for (Object o : products) {
            JSONObject productObj = (JSONObject) o;

            String internalPid = CommonMethods.getLast(productObj.optString("productSku").split("_"));
            String urlProduct = "https://www.martinsatacado.com.br" + productObj.optString("productUrl");
            String name = productObj.getString("name");
            String imageUrl = JSONUtils.getValueRecursive(productObj, "images.0.value", String.class);
            Integer price = getPrice(productObj.optString("productSku"));
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(urlProduct)
               .setInternalPid(internalPid)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
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

   protected void login() {

      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
      headers.put("authority", "www.martinsatacado.com.br");
      headers.put("Authorization", "Basic YmI2ZDhiZTgtMDY3MS0zMmVhLTlhNmUtM2RhNGM2MzUyNWEzOmJmZDYxMTdlLWMwZDMtM2ZjNS1iMzc3LWFjNzgxM2Y5MDY2ZA==");
      String payload = "{\"grant_type\":\"password\",\"cnpj\":\"" + cnpj + "\",\"username\":\"" + getLogin() + "\",\"codCli\":\"6659973\",\"password\":\"" + getPassword() + "\",\"codedevnd\":\"\",\"profile\":\"ROLE_CLIENT\"}";

      Request request = Request.RequestBuilder.create()
         .setUrl("https://ssd.martins.com.br/oauth-marketplace-portal/access-tokens")
         .setPayload(payload)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY))
         .setHeaders(headers)
         .build();
      Response response = this.dataFetcher.post(session, request);
      String str = response.getBody();
      JSONObject body = JSONUtils.stringToJson(str);
      accessToken = body.optString("access_token");
   }

   protected void fetchPrices(JSONArray products) {
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
      headers.put("Origin", "www.martinsatacado.com.br");
      headers.put("access_token", accessToken);
      headers.put("client_id", "bb6d8be8-0671-32ea-9a6e-3da4c63525a3");
      headers.put("Authorization", "Basic YmI2ZDhiZTgtMDY3MS0zMmVhLTlhNmUtM2RhNGM2MzUyNWEzOmJmZDYxMTdlLWMwZDMtM2ZjNS1iMzc3LWFjNzgxM2Y5MDY2ZA==");
      String payload = "{\"asm\":0,\"produtos\":[],\"ProdutosExclusaoEan\":[],\"produtosSeller\":[" + getPayload(products) + "],\"codeWarehouseDelivery\":0,\"codeWarehouseBilling\":0,\"condicaoPagamento\":111,\"uid\":6659973,\"segment\":0,\"tipoLimiteCred\":\"C\",\"precoEspecial\":\"S\",\"ie\":\"127285489112\",\"territorioRca\":0,\"classEstadual\":10,\"tipoSituacaoJuridica\":\"M\",\"codSegNegCliTer\":0,\"tipoConsulta\":1,\"commercialActivity\":5,\"groupMartins\":171,\"codCidadeEntrega\":3232,\"codCidade\":3232,\"codRegiaoPreco\":250,\"temVendor\":\"S\",\"codigoCanal\":9,\"ufTarget\":\"SP\",\"bu\":1,\"manual\":\"N\",\"email\":\"patriciaf3001@gmail.com\",\"numberSpinPrice\":\"64\",\"codeDeliveryRegion\":\"322\",\"ufFilialFaturamento\":\"GO\",\"cupons_novos\":[],\"codopdtrcetn\":10,\"origemChamada\":\"PLP\"}";

      Request request = Request.RequestBuilder.create()
         .setUrl("https://ssd.martins.com.br/b2b-partner/v1/produtosBuyBox")
         .setPayload(payload)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY))
         .setHeaders(headers)
         .build();
      Response response = this.dataFetcher.post(session, request);
      String str = response.getBody();
      JSONObject body = JSONUtils.stringToJson(str);
      this.prices = JSONUtils.getValueRecursive(body, "lstPrecoSeller", JSONArray.class);
   }

   protected String getPayload(JSONArray products) {
      String payload = "";
      Boolean flag = false;
      for (Object o : products) {
         JSONObject product = (JSONObject) o;
         String cod = product.optString("productSku");
         List<String> parts = List.of(cod.split("_"));
         if(flag){
            payload = payload +",";
         }
         payload = payload + "{\"seller\":\"" + parts.get(0) + "\",\"CodigoMercadoria\":\"" + cod + "\",\"Quantidade\":0}";
         flag = true;
      }

      return payload;
   }

   protected Integer getPrice(String id) {

      for (Object o : this.prices) {
         JSONObject price = (JSONObject) o;
         String cod = price.getString("codigoMercadoria");
         if (cod.equals(id)) {
            String priceStr = price.optString("preco");
            priceStr = priceStr.replaceAll("\\.", "");
            Integer priceInt = Integer.parseInt(priceStr);
            return priceInt;
         }
      }


      return null;
   }

   protected String fetchJson() {
      login();
      String id = catureId();
      String url = "https://www.martinsatacado.com.br/_next/data/"+ id +"/busca/" + this.keywordWithoutAccents.replace(" ", "%20") + ".json?page=" + this.currentPage + "&perPage=" + this.pageSize;
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY))
         .build();
      String response = this.dataFetcher.get(session, request).getBody();

      return response;
   }
   protected String catureId(){
      String id = null;
      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.martinsatacado.com.br/")
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY))
         .build();
      String response = this.dataFetcher.get(session, request).getBody();
      Document doc = Jsoup.parse(response);
      String script = CrawlerUtils.scrapScriptFromHtml(doc,"#__NEXT_DATA__");
      JSONArray obj = JSONUtils.stringToJsonArray(script);
      id = JSONUtils.getValueRecursive(obj,"0.buildId", String.class);
      return id;
   }

}
