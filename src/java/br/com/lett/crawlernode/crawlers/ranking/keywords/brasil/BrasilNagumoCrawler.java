package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrasilNagumoCrawler extends BrasilSitemercadoCrawler {

   private Map<String, Integer> lojaInfo = getLojaInfo();
   private Double latitude = session.getOptions().optDouble("latitude");
   private Double longitude = session.getOptions().optDouble("longitude");

   public BrasilNagumoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String apiSearchUrl(String lojaId) {
      return "https://www.nagumo.com.br/api/b2c/product?store_id=" + lojaId + "&text=";
   }

   @Override
   protected Map<String, Integer> getLojaInfo() {
      return super.getLojaInfo();
   }

   @Override
   protected JSONObject crawlProductInfo() {
      String apiUrl = apiSearchUrl(lojaInfo.get("IdLoja").toString()) + this.keywordEncoded.replace("+", "%20");

      Map<String, String> headers = new HashMap<>();
      headers.put("hosturl", "www.nagumo.com.br");
      headers.put("Accept", "application/json, text/plain, */*");
      headers.put("sm-token", "{\"Location\":{\"Latitude\":" + latitude + ",\"Longitude\":" + longitude + "},\"IdLoja\":" + lojaInfo.get("IdLoja") + ",\"IdRede\":" + lojaInfo.get("IdRede") + "}");

      Request requestApi = Request.RequestBuilder.create()
         .setUrl(apiUrl)
         .setProxyservice(List.of(ProxyCollection.BUY, ProxyCollection.NETNUT_RESIDENTIAL_BR, ProxyCollection.LUMINATI_RESIDENTIAL_BR))
         .setCookies(cookies)
         .setHeaders(headers)
         .build();

      return CrawlerUtils.stringToJson(this.dataFetcher.get(session, requestApi).getBody());
   }
}
