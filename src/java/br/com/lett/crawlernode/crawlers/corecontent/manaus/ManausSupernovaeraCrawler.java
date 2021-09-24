package br.com.lett.crawlernode.crawlers.corecontent.manaus;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import models.RatingsReviews;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Arrays;
import java.util.List;

public class ManausSupernovaeraCrawler extends VTEXOldScraper {

   private static final String HOME_PAGE = "https://www.supernovaera.com.br/";
   private static final String MAIN_SELLER_NAME = "mercantil nova era ltda";
   protected final String storeId = getStoreId();

   protected String getStoreId() {
      return session.getOptions().optString("store_id");
   }

   public ManausSupernovaeraCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public void handleCookiesBeforeFetch() {
      Request request = Request.RequestBuilder.create().setUrl(HOME_PAGE + "?sc=" + storeId).setCookies(cookies).build();
      Response response = this.dataFetcher.get(session, request);

      for (Cookie cookieResponse : response.getCookies()) {
         BasicClientCookie cookie = new BasicClientCookie(cookieResponse.getName(), cookieResponse.getValue());
         cookie.setDomain("www.supernovaera.com.br");
         cookie.setPath("/");
         this.cookies.add(cookie);
      }
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Arrays.asList(MAIN_SELLER_NAME);
   }

   @Override
   protected JSONObject crawlProductApi(String internalPid, String parameters) {
      return super.crawlProductApi(internalPid, "&sc=" + storeId);
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }
}

