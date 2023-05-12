package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SiteMercadoCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import org.apache.http.HttpHeaders;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BrasilNagumoCrawler extends SiteMercadoCrawler {

   private final String API_URL = "https://www.nagumo.com.br/api/b2c/";

   private Map<String, Integer> lojaInfo = getLojaInfo();
   private Double latitude = session.getOptions().optDouble("latitude");
   private Double longitude = session.getOptions().optDouble("longitude");

   public BrasilNagumoCrawler(Session session) {
      super(session);
   }

   @Override
   protected Map<String, Integer> getLojaInfo() {
      return super.getLojaInfo();
   }

   @Override
   protected Response fetchResponse() {
      return crawlProductInformatioFromApi(session.getOriginalURL());
   }

   @Override
   protected Response crawlProductInformatioFromApi(String productUrl) {
      String productName = CommonMethods.getLast(productUrl.split("/")).split("\\?")[0];
      String url = API_URL + "product/" + productName + "?store_id=" + lojaInfo.get("IdLoja");

      Map<String, String> headers = new HashMap<>();
      headers.put("hosturl", "www.nagumo.com.br");
      headers.put(HttpHeaders.ACCEPT, "application/json, text/plain, */*");
      headers.put("sm-token", "{\"Location\":{\"Latitude\":" + latitude + ",\"Longitude\":" + longitude + "},\"IdLoja\":" + lojaInfo.get("IdLoja") + ",\"IdRede\":" + lojaInfo.get("IdRede") + "}");

      Request requestApi = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(ProxyCollection.BUY, ProxyCollection.LUMINATI_RESIDENTIAL_BR, ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY))
         .build();

      return this.dataFetcher.get(session, requestApi);
   }
}
