package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MartinsKeywords extends CrawlerRankingKeywords {

   public MartinsKeywords(Session session) {
      super(session);
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
            if (this.totalProducts == 0) {
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
                  String urlProduct = "https://www.martinsatacado.com.br" + productObj.optString("productUrl") + "-" + id;
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
      String payload = "{\"grant_type\":\"password\",\"cnpj\":\"" + cnpj + "\",\"username\":\"" + getLogin() + "\",\"codCli\":\"" + codCli + "\",\"password\":\"" + getPassword() + "\",\"codedevnd\":\"\",\"profile\":\"ROLE_CLIENT\"}";

      HttpResponse<String> response;
      try {
         HttpClient client = HttpClient.newBuilder().build();
         HttpRequest request = HttpRequest.newBuilder()
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .uri(URI.create("https://ssd.martins.com.br/oauth-marketplace-portal/access-tokens"))
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .header("authority", "www.martinsatacado.com.br")
            .header("Authorization", "Basic YmI2ZDhiZTgtMDY3MS0zMmVhLTlhNmUtM2RhNGM2MzUyNWEzOmJmZDYxMTdlLWMwZDMtM2ZjNS1iMzc3LWFjNzgxM2Y5MDY2ZA==")
            .build();
         response = client.send(request, HttpResponse.BodyHandlers.ofString());
      } catch (Exception e) {
         throw new RuntimeException("Failed In load document: " + session.getOriginalURL(), e);
      }
      JSONObject body = JSONUtils.stringToJson(response.body());
      accessToken = body.optString("access_token");
   }

   protected JSONArray fetchPrices(JSONArray products) {
      JSONArray prices = new JSONArray();
      HttpResponse<String> response;
      String payload = "{\"produtos\":[" + getPayload(products) + "],\"uid\":" + codCli + ",\"ufTarget\":\"SP\",\"email\":\"" + login + "\"}";
      if (accessToken == null || accessToken.isEmpty()) {
         login();
      }
      if (accessToken != null && !accessToken.isEmpty()) {

         try {
            HttpClient client = HttpClient.newBuilder().build();
            HttpRequest request = HttpRequest.newBuilder()
               .POST(HttpRequest.BodyPublishers.ofString(payload))
               .header("access_token", accessToken)
               .header(HttpHeaders.CONTENT_TYPE, "application/json")
               .header("Origin", "www.martinsatacado.com.br")
               .header("client_id", "bb6d8be8-0671-32ea-9a6e-3da4c63525a3")
               .header("Authorization", "Basic YmI2ZDhiZTgtMDY3MS0zMmVhLTlhNmUtM2RhNGM2MzUyNWEzOmJmZDYxMTdlLWMwZDMtM2ZjNS1iMzc3LWFjNzgxM2Y5MDY2ZA==")
               .uri(URI.create("https://ssd.martins.com.br/b2b-partner/v1/produtosBuyBox"))
               .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());
         } catch (Exception e) {
            throw new RuntimeException("Failed In load document: " + session.getOriginalURL(), e);
         }
         JSONObject body = JSONUtils.stringToJson(response.body());
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
      try {
         HttpClient client = HttpClient.newBuilder().build();
         HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(url))
            .build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         return JSONUtils.stringToJson(response.body());
      } catch (Exception e) {
         throw new RuntimeException("Failed In load document: " + session.getOriginalURL(), e);
      }

   }

   protected String captureIdToRequestApi() {
      String id = null;
      HttpResponse<String> response;
      try {
         HttpClient client = HttpClient.newBuilder().build();
         HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create("https://www.martinsatacado.com.br/"))
            .build();
         response = client.send(request, HttpResponse.BodyHandlers.ofString());

      } catch (Exception e) {
         throw new RuntimeException("Failed In load document: " + session.getOriginalURL(), e);
      }
      Document doc = Jsoup.parse(response.body());
      if (doc != null) {
         JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "#__NEXT_DATA__", null, null, false, false);
         if (json != null) {
            id = json.optString("buildId");
         }

      }
      return id;
   }

}
