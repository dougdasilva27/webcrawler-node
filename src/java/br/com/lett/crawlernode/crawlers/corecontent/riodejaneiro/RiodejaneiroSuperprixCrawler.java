package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import java.util.*;

import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import models.RatingsReviews;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;

public class RiodejaneiroSuperprixCrawler extends VTEXOldScraper {

   private static final String HOME_PAGE = "https://www.ipanema.superprix.com.br/";
   private static final String CEP = "22041080";
   private static final String CITY = "RIO DE JANEIRO";
   private static final String LOCATION = "4";


   public RiodejaneiroSuperprixCrawler(Session session) {
      super(session);
   }

   @Override
   protected Object fetch() {
//      List<Cookie> cookiesFetc = Arrays.asList(new BasicClientCookie("VTEXSC", "sc=" + LOCATION),
//         new BasicClientCookie("userCep", CEP),
//         new BasicClientCookie("cityValidation", CITY));

      cookies.addAll(Arrays.asList(new BasicClientCookie("VTEXSC", "sc=" + LOCATION),
         new BasicClientCookie("userCep", CEP),
         new BasicClientCookie("cityValidation", CITY)));

      Request request = Request.RequestBuilder.create().setUrl(session.getOriginalURL()).setCookies(cookies).build();
      String response = dataFetcher.get(session, request).getBody();

      return Jsoup.parse(response);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Collections.singletonList("superprixipanema");
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }

   @Override
   protected JSONObject crawlProductApi(String internalPid, String parameters) {
      return super.crawlProductApi(internalPid, null);
   }

}
