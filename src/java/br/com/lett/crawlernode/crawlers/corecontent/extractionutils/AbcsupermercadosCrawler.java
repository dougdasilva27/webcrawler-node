package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import models.RatingsReviews;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;

import java.util.Collections;
import java.util.List;

public abstract class AbcsupermercadosCrawler extends VTEXOldScraper{

   private static final String HOME_PAGE = "https://www.superabc.com.br/";
   private static final List<Cookie> COOKIES = new ArrayList<>();
   private static final List<String> MAIN_SELLERS = Collections.singletonList("Supermercado ABC");

   public AbcsupermercadosCrawler(Session session) {
      super(session);
   }

   protected abstract String getLocation();

   @Override
   protected Object fetch(){

      BasicClientCookie cookie = new BasicClientCookie("VTEXSC", "sc=" + getLocation());
      cookie.setDomain("www.superabc.com.br");
      cookie.setPath("/");
      COOKIES.add(cookie);

      Request request = Request.RequestBuilder.create().setUrl(session.getOriginalURL()).setCookies(COOKIES).build();
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
      return new RatingsReviews();
   }

   @Override
   protected JSONObject crawlProductApi(String internalPid, String parameters) {
      return super.crawlProductApi(internalPid, "&sc=" + getLocation());
   }
}
