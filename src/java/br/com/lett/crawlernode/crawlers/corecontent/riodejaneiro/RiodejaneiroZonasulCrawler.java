package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.RatingsReviews;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * Date: 04/01/2021
 *
 * @author Marcos Moura
 */
public class RiodejaneiroZonasulCrawler extends VTEXNewScraper {

   private static final String HOME_PAGE = "https://www.zonasul.com.br/";
   private static final String SELLER_NAME = "Super Mercado Zona Sul S/A";

   public RiodejaneiroZonasulCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
      //  super.config.setParser(Parser.HTML);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public void handleCookiesBeforeFetch() {

      BasicClientCookie userLocationData = new BasicClientCookie("vtex_segment", session.getOptions().optString("vtex_segment"));
      userLocationData.setPath("/");
      cookies.add(userLocationData);
   }

   @Override
   protected JSONObject crawlProductApi(String internalPid, String parameters) {
      JSONObject productApi = new JSONObject();

      String url = homePage + "api/catalog_system/pub/products/search?fq=productId:" + internalPid + (parameters == null ? "" : parameters);

      Map<String, String> headers = new HashMap<>();
      headers.put("cookie", session.getOptions().optString("cookies"));

      Request request = Request.RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).build();
      JSONArray array = CrawlerUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());

      if (!array.isEmpty()) {
         productApi = array.optJSONObject(0) == null ? new JSONObject() : array.optJSONObject(0);
      }

      return productApi;
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Arrays.asList(SELLER_NAME);
   }

   @Override
   protected String scrapDescription(Document doc, JSONObject productJson) throws UnsupportedEncodingException {
      return CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList("div.vtex-tab-layout-0-x-container--information .vtex-tab-layout-0-x-contentContainer"));
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }

   @Override
   public String scrapInternalPid(Document doc, JSONObject jsonObject, String pidFromApi) {
      return jsonObject.optString("productReference");
   }


}
