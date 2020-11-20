package br.com.lett.crawlernode.crawlers.corecontent.saoluiz;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import models.RatingsReviews;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Collections;
import java.util.List;

public class SaoluizMateusonlineCrawler extends VTEXOldScraper {

   private static final String HOME_PAGE = "https://www.mateusonline.com.br/";
   private static final List<String> MAIN_SELLER_NAME_LOWER = Collections.singletonList("Mateus Online");
   private static final Integer STORE_ID = 1;

   public SaoluizMateusonlineCrawler(Session session) {
      super(session);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      String url = "https://www.mateusonline.com.br/Site/Track.aspx?sc=" + STORE_ID;

      Request request = Request.RequestBuilder.create().setUrl(url).build();
      Response response = this.dataFetcher.get(session, request);

      List<Cookie> cookiesResponse = response.getCookies();

      for (Cookie cookieResponse : cookiesResponse) {
         BasicClientCookie cookie = new BasicClientCookie(cookieResponse.getName(), cookieResponse.getValue());
         cookie.setDomain("www.mateusonline.com.br");
         cookie.setPath("/");
         cookies.add(cookie);
      }
   }

   @Override
   public String handleURLBeforeFetch(String url) {
      return super.handleURLBeforeFetch(url.split("\\?")[0] + "?sc=" + STORE_ID);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return MAIN_SELLER_NAME_LOWER;
   }

   @Override
   protected String scrapDescription(Document doc, JSONObject productJson) {
      return JSONUtils.getStringValue(productJson, "description") +
         CrawlerUtils.scrapElementsDescription(doc, Collections.singletonList("#caracteristicas"));
   }

   //When this crawler was made no product with rating was found
   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return new RatingsReviews();
   }
}
