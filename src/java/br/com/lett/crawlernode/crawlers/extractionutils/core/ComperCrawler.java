
package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.RatingsReviews;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class ComperCrawler extends VTEXNewScraper {

   private static final String HOME_PAGE = "https://www.comper.com.br/";
   private static final String MAIN_SELLER_NAME = "sdb comercio de alimentos ltda.";
   protected final String storeId = getStoreId();

   protected abstract String getStoreId();

   public ComperCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public void handleCookiesBeforeFetch() {
      BasicClientCookie cookie = new BasicClientCookie("VTEXSC", "sc=" + getStoreId());
      cookie.setDomain("www.comper.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);

      BasicClientCookie cookie2 = new BasicClientCookie("nav_id", session.getOptions().optString("nav_id","bde25fd2-81a2-45ca-955d-1f3162b195c5"));
      cookie2.setDomain("www.comper.com.br");
      cookie2.setPath("/");
      this.cookies.add(cookie2);
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
   protected String scrapDescription(Document doc, JSONObject productJson) {
      StringBuilder description = new StringBuilder();

      if (productJson.has("description")) {
         description.append(productJson.get("description").toString());
      }

      List<String> specs = new ArrayList<>();

      if (productJson.has("allSpecifications")) {
         JSONArray keys = productJson.getJSONArray("allSpecifications");
         for (Object o : keys) {
            if (!o.toString().equalsIgnoreCase("Informações para Instalação") && !o.toString().equalsIgnoreCase("Portfólio")) {
               specs.add(o.toString());
            }
         }
      }

      for (String spec : specs) {

         description.append("<div>");
         description.append("<h4>").append(spec).append("</h4>");
         description.append(sanitizeDescription(productJson.get(spec)));
         description.append("</div>");
      }

      return description.toString();
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }
}
