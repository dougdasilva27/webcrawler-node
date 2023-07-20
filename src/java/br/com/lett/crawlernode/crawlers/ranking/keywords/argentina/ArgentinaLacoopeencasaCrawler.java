package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.JSONUtils;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArgentinaLacoopeencasaCrawler extends CrawlerRankingKeywords {
   private static final String HOME_PAGE = "https://www.lacoopeencasa.coop/";

   public ArgentinaLacoopeencasaCrawler(Session session) {
      super(session);
   }

   private String cookieSecurity = null;
   private final String locationCookie = getLocation();

   protected String getLocation() {
      return session.getOptions().optString("location_cookie");
   }

   @Override
   protected void processBeforeFetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put("Accept", "*/*");
      headers.put("Host", "www.lacoopeencasa.coop");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.lacoopeencasa.coop/ws/index.php/comun/autentificacionController/autentificar_invitado")
         .setHeaders(headers)
         .setPayload("{}")
         .setProxyservice(List.of(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_ROTATE_AR))
         .build();

      Response response = new FetcherDataFetcher().post(session, request);
      List<Cookie> cookiesResponse = response.getCookies();
      for (Cookie cookieResponse : cookiesResponse) {
         if (cookieResponse.getName().equalsIgnoreCase("_lcec_sid_inv")) {
            this.cookieSecurity = cookieResponse.getValue();
         }
      }
   }

   @Override
   protected JSONObject fetchJSONObject(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("Accept", "application/json, text/plain, */*");
      headers.put("Host", "www.lacoopeencasa.coop");
      headers.put("Content-Type", "application/json");
      headers.put("Cookie", "_lcec_sid_inv=" + this.cookieSecurity + ";_lcec_linf=" + locationCookie + ";");

      String payload = "{\"pagina\":" + (this.currentPage - 1) + ",\"filtros\":{\"preciomenor\":-1,\"preciomayor\":-1,\"categoria\":[],\"marca\":[],\"tipo_seleccion\":\"busqueda\"," +
         "\"tipo_relacion\":\"busqueda\",\"filtros_gramaje\":[],\"termino\":\"" + this.keywordEncoded + "\",\"cant_articulos\":0,\"ofertas\":false,\"modificado\":false,\"primer_filtro\":\"\"}}";

      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.lacoopeencasa.coop/ws/index.php/categoria/categoriaController/filtros_busqueda")
         .setHeaders(headers)
         .setPayload(payload)
         .setProxyservice(List.of(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_ROTATE_AR))
         .build();

      Response response = new FetcherDataFetcher().post(session, request);
      return JSONUtils.stringToJson(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 32;
      String url = "https://www.lacoopeencasa.coop/ws/index.php/categoria/categoriaController/filtros_busqueda";
      JSONObject productsJson = fetchJSONObject(url);

      JSONArray products = JSONUtils.getValueRecursive(productsJson, "datos.articulos", JSONArray.class);
      this.totalProducts = JSONUtils.getValueRecursive(productsJson, "datos.cantidad_articulos", Integer.class);

      if (products != null) {
         for (Object p : products) {
            if (p instanceof JSONObject) {
               JSONObject product = (JSONObject) p;
               String internalId = product.optString("cod_interno");
               String name = product.optString("descripcion");
               String productUrl = scrapProductUrl(name, internalId);
               String image = product.optString("imagen");
               boolean available = product.optInt("stock") > 0;
               Integer priceInCents = available ? crawlPrice(product) : null;

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setInternalId(internalId)
                  .setName(name)
                  .setUrl(productUrl)
                  .setImageUrl(image)
                  .setAvailability(available)
                  .setPriceInCents(priceInCents)
                  .build();

               saveDataProduct(productRanking);
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultados!");
      }

   }

   private Integer crawlPrice(JSONObject product) {
      String price = JSONUtils.getStringValue(product, "precio_promo") == null ? JSONUtils.getStringValue(product, "precio") : JSONUtils.getStringValue(product, "precio_promo");

      double doubleValue = Double.parseDouble(price);
      double centsDouble = doubleValue * 100;
      return (int) centsDouble;
   }

   private String scrapProductUrl(String name, String internalId) {
      String field = name.toLowerCase().replace(" ", "-");
      return HOME_PAGE + "producto/" + field + "/" + internalId;
   }
}
