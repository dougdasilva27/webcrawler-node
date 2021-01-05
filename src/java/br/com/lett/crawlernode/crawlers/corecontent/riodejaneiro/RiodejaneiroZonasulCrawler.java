package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.RatingsReviews;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Date: 04/01/2021
 *
 * @author Marcos Moura
 */
public class RiodejaneiroZonasulCrawler extends VTEXNewScraper {

  private static final String HOME_PAGE = "https://www.zonasul.com.br/";
  private static final String SELLER_NAME = "Super Mercado Zona Sul S/A";
  private static final String COOKIES = "azion_balancer=B; vtex_session=eyJhbGciOiJFUzI1NiIsImtpZCI6IjA5RDFDRDUwRDM5RjVFREVCQzU0ODc0RUEyQkQ3RkZEQzIxNzVEQUQiLCJ0eXAiOiJqd3QifQ.eyJhY2NvdW50LmlkIjoiMmIyYjYxMTktNjM0Zi00ZjRiLWJmYzQtMmE0Y2Y5YzdiNTEzIiwiaWQiOiIyNWNmYTIwZS1mNjQ1LTQ5NGEtYTgyMC1iZTdmMzBhM2Q3ZDMiLCJ2ZXJzaW9uIjoyLCJzdWIiOiJzZXNzaW9uIiwiYWNjb3VudCI6InNlc3Npb24iLCJleHAiOjE2MTA0NzUwNjMsImlhdCI6MTYwOTc4Mzg2MywiaXNzIjoidG9rZW4tZW1pdHRlciIsImp0aSI6IjQyOWQzNGViLTg0YTUtNGQyZC1iZWFiLTNmMWRmNTYyY2ZmZSJ9.tAaLBagIcJP7FTQZc6QnYSIo60jgRpiVjPtobRYgxiZrNWtJ2kJn3mily06ZGxEhUFsI0uPv2993eoAA3ehD2A";
  //This is not the best way to set cookies but it was the only way found when this crawler was created

   public RiodejaneiroZonasulCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public void handleCookiesBeforeFetch() {
      super.handleCookiesBeforeFetch();
   }

   @Override
   protected Object fetch() {

      Map<String,String> headers = new HashMap<>();
      headers.put("cookie", COOKIES);

      Request request = Request.RequestBuilder.create().setUrl(session.getOriginalURL()).setHeaders(headers).build();
      Document doc = Jsoup.parse(dataFetcher.get(session,request).getBody());

      return doc;
   }

   @Override
   protected JSONObject crawlProductApi(String internalPid, String parameters) {
      JSONObject productApi = new JSONObject();

      String url = homePage + "api/catalog_system/pub/products/search?fq=productId:" + internalPid + (parameters == null ? "" : parameters);

      Map<String,String> headers = new HashMap<>();
      headers.put("cookie", COOKIES);

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
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }
}
