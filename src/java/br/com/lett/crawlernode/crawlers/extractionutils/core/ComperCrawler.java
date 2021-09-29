
package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class ComperCrawler extends VTEXOldScraper {

   private static final String HOME_PAGE = "https://www.comper.com.br/";
   private static final String MAIN_SELLER_NAME = "sdb comercio de alimentos ltda.";
   protected final String storeId = getStoreId();

   protected abstract String getStoreId();

   public ComperCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.JSOUP);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   protected Object fetch(){

      BasicClientCookie cookie = new BasicClientCookie("VTEXSC", "sc=" + getStoreId());
      cookie.setDomain("www.comper.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);

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
      return Arrays.asList(MAIN_SELLER_NAME);
   }

   @Override
   protected JSONObject crawlProductApi(String internalPid, String parameters) {
      return super.crawlProductApi(internalPid, "&sc=" + storeId);
   }

//   @Override
//   protected String scrapInternalpid(Document doc) {
//      return CrawlerUtils.scrapStringSimpleInfo(doc, ".av-row.av-row-flex .skuReference", true);
//   }

   @Override
   protected List<String> scrapImages(Document doc, JSONObject skuJson, String internalPid, String internalId) {
      return super.scrapImagesOldWay(internalId);
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
