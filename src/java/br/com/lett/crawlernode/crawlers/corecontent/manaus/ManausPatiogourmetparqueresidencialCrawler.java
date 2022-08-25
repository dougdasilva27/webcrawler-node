package br.com.lett.crawlernode.crawlers.corecontent.manaus;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.RatingsReviews;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Arrays;
import java.util.List;

public class ManausPatiogourmetparqueresidencialCrawler extends VTEXOldScraper {

   private final String homePage = getHomePage();
   private static final String MAIN_SELLER_NAME = "Pátio Gourmet";
   protected final String storeId = getStoreId();

   protected String getStoreId() {
      return session.getOptions().optString("store_id");
   }

   public ManausPatiogourmetparqueresidencialCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(homePage));
   }

   @Override
   public void handleCookiesBeforeFetch() {
      Request request = Request.RequestBuilder.create().setUrl(homePage + "?sc=" + storeId).setCookies(cookies).build();
      Response response = new ApacheDataFetcher().get(session, request);

      for (Cookie cookieResponse : response.getCookies()) {
         BasicClientCookie cookie = new BasicClientCookie(cookieResponse.getName(), cookieResponse.getValue());
         cookie.setDomain("www.patiogourmet.com.br");
         cookie.setPath("/");
         this.cookies.add(cookie);
      }
   }

   @Override
   protected Object fetch() {
      Request request = Request.RequestBuilder.create()
         .setCookies(this.cookies)
         .setUrl(session.getOriginalURL())
         .setProxyservice(List.of(
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.BUY_HAPROXY
         ))
         .setFollowRedirects(true)
         .setTimeout(8000)
         .build();

      Response response = CrawlerUtils.retryRequest(request, session, new ApacheDataFetcher(), true);

      return response;
   }

   @Override
   protected String getHomePage() {
      return session.getOptions().optString("home_page");
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Arrays.asList(MAIN_SELLER_NAME);
   }

   @Override
   protected JSONObject crawlProductApi(String internalPid, String parameters) {
      JSONObject productApi = new JSONObject();

      String url = homePage + "api/catalog_system/pub/products/search?fq=productId:" + internalPid + "&sc=" + storeId;

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(this.cookies)
         .setProxyservice(List.of(
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.BUY_HAPROXY
         ))
         .setTimeout(8000)
         .build();

      JSONArray array = CrawlerUtils.stringToJsonArray(CrawlerUtils.retryRequest(request, session, this.dataFetcher, true).getBody());

      if (!array.isEmpty()) {
         productApi = array.optJSONObject(0) == null ? new JSONObject() : array.optJSONObject(0);
      }

      return productApi;
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }
}
