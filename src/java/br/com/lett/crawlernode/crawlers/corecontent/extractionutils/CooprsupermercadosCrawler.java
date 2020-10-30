package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.RatingsReviews;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public abstract class CooprsupermercadosCrawler extends VTEXOldScraper{


   private static final String HOME_PAGE = "https://www.cooplojaonline.com.br/";
   private static final List<Cookie> COOKIES = new ArrayList<>();
   private static final List<String> MAIN_SELLERS = Collections.singletonList("coopsp");

   public CooprsupermercadosCrawler(Session session) {
      super(session);
   }

   protected abstract String getLocation();

   @Override
   public void handleCookiesBeforeFetch() {

      String payload = "{\"public\":{\"country\":{\"value\":\"BRA\"},\"regionId\":{\"value\":\"" + getLocation() +"\"}}}";

      HashMap<String, String> headers = new HashMap<>();
      headers.put("Content-type", "application/json");

      Request request = Request.RequestBuilder.create().setUrl("https://www.cooplojaonline.com.br/api/sessions/").setHeaders(headers).setPayload(payload)
         .build();
      Response response = this.dataFetcher.post(session, request);

      for (Cookie cookieResponse : response.getCookies()) {
         BasicClientCookie cookie = new BasicClientCookie(cookieResponse.getName(), cookieResponse.getValue());
         cookie.setDomain("www.cooplojaonline.com.br");
         cookie.setPath("/");
         this.cookies.add(cookie);
      }
   }

   @Override
   protected Object fetch(){

      Request request = Request.RequestBuilder.create().setUrl(session.getOriginalURL()).setCookies(this.cookies).build();
      String response = dataFetcher.get(session, request).getBody();

      return Jsoup.parse(response);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return MAIN_SELLERS;
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }
}
