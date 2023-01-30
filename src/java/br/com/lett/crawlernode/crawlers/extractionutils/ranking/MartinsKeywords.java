package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
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
   protected String ufBilling = getUfBilling();
   protected String filDelivery = getFilDelivery();
   protected String codCli = getCodCli();

   protected String getCodCli() {
      return session.getOptions().optString("cod_cliente");
   }

   protected String getPassword() {
      return session.getOptions().optString("pass");
   }

   protected String getCnpj() {
      return session.getOptions().optString("cnpj");
   }

   protected String getLogin() {
      return session.getOptions().optString("login");
   }

   protected String getUfBilling() {
      return session.getOptions().optString("uf_billing");
   }

   protected String getFilDelivery() {
      return session.getOptions().optString("fil_delivery");
   }


   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);
      JSONObject obj = fetchJson();
      if (obj != null) {
         obj = JSONUtils.getValueRecursive(obj, "pageProps.fallback./api/search.searchResult", JSONObject.class);
         if (obj != null) {
            JSONArray products = obj.getJSONArray("products");
            if(this.totalProducts == 0){
               this.totalProducts = obj.optInt("productTotal");
            }
            if (!products.isEmpty()) {
               JSONArray prices = fetchPrices(products);
               for (Object o : products) {
                  JSONObject productObj = (JSONObject) o;
                  String internalPid = null;
                  String id = productObj.optString("productSku");
                  if (id != null) {
                     internalPid = CommonMethods.getLast(id.split("_"));
                  }
                  String urlProduct = "https://www.martinsatacado.com.br" + productObj.optString("productUrl");
                  String name = productObj.optString("name");
                  String imageUrl = JSONUtils.getValueRecursive(productObj, "images.0.value", String.class);
                  Integer price = getPrice(id, prices);
                  boolean isAvailable = price != null;

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
      String payload = "{\"grant_type\":\"password\",\"cnpj\":\"" + cnpj + "\",\"username\":\"" + getLogin() + "\",\"codCli\":\"" + codCli + "\",\"password\":\"" + getPassword() + "\",\"codedevnd\":\"\",\"profile\":\"ROLE_CLIENT\"}";

      Request request = Request.RequestBuilder.create()
         .setUrl("https://ssd.martins.com.br/oauth-marketplace-portal/access-tokens")
         .setPayload(payload)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY))
         .setHeaders(headers)
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(this.dataFetcher, new ApacheDataFetcher(), new FetcherDataFetcher()), session, "post");
      JSONObject body = JSONUtils.stringToJson(response.getBody());
      accessToken = body.optString("access_token");
   }

   protected JSONArray fetchPrices(JSONArray products) {
      JSONArray prices = new JSONArray();
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
      headers.put("Origin", "www.martinsatacado.com.br");
      headers.put("client_id", "bb6d8be8-0671-32ea-9a6e-3da4c63525a3");
      headers.put("host", "ssd.martins.com.br");
      headers.put("Authorization", "Basic YmI2ZDhiZTgtMDY3MS0zMmVhLTlhNmUtM2RhNGM2MzUyNWEzOmJmZDYxMTdlLWMwZDMtM2ZjNS1iMzc3LWFjNzgxM2Y5MDY2ZA==");
      String payload = "{\"produtos\":[" + getPayload(products) + "],\"uid\":" + codCli + ",\"ufTarget\":\"SP\",\"email\":\"" + login + "\"}";
      if (accessToken == null || accessToken.isEmpty()) {
         login();
      }
      if (accessToken != null && !accessToken.isEmpty()) {
         headers.put("access_token", accessToken);

         Request request = Request.RequestBuilder.create()
            .setUrl("https://ssd.martins.com.br/b2b-partner/v1/produtosBuyBox")
            .setPayload(payload)
            .setProxyservice(Arrays.asList(
               ProxyCollection.BUY_HAPROXY,
               ProxyCollection.BUY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR,
               ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY))
            .setHeaders(headers)
            .build();

         Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(this.dataFetcher, new ApacheDataFetcher(), new FetcherDataFetcher()), session, "post");
         JSONObject body = JSONUtils.stringToJson(response.getBody());
         if (body != null) {
            prices = body.optJSONArray("resultado");
         }
      }

      return prices;
   }

   protected String getPayload(JSONArray products) {
      String payload = "";
      Boolean flag = false;
      for (Object o : products) {
         JSONObject product = (JSONObject) o;
         String cod = product.optString("productSku");
         if (flag) {
            payload = payload + ",";
         }
         payload = payload + "{\"CodigoMercadoria\":\"" + cod + "\"}";
         flag = true;
      }

      return payload;
   }

   protected Integer getPrice(String id, JSONArray prices) {
      if (prices != null) {
         for (Object o : prices) {
            JSONObject price = (JSONObject) o;
            String cod = price.optString("codigoMercadoria");
            if (cod.equals(id)) {
               JSONArray priceProducts = price.optJSONArray("precos");
               for (Object o1 : priceProducts) {
                  if (o1 instanceof JSONObject) {
                     JSONObject priceProduct = (JSONObject) o1;
                     String uf = priceProduct.optString("uf_Billing");
                     String seller = priceProduct.optString("fil_delivery");

                     if (ufBilling.equals(uf) && filDelivery.equals(seller)) {
                        Double priceNormal = priceProduct.optDouble("precoNormal");
                        Integer priceInt = CommonMethods.doublePriceToIntegerPrice(priceNormal, null);
                        if (priceInt == 0) {
                           priceInt = null;
                        }

                        return priceInt;
                     }
                  }
               }
               break;
            }
         }

      }

      return null;
   }

   protected JSONObject fetchJson() {
      login();
      String id = captureIdToRequestApi();
      String url = "https://www.martinsatacado.com.br/_next/data/" + id + "/busca/" + this.keywordWithoutAccents.replace(" ", "%20") + ".json?page=" + this.currentPage + "&perPage=" + this.pageSize;
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY))
         .build();

      return JSONUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

   }

   protected String captureIdToRequestApi() {
      String id = null;
      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.martinsatacado.com.br/")
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY))
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session);
      Document doc = Jsoup.parse(response.getBody());
      if (doc != null) {
         JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "#__NEXT_DATA__", null, null, false, false);
         if (json != null) {
            id = json.optString("buildId");
         }

      }
      return id;
   }

}
