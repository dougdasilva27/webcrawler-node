package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JavanetDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.RatingsReviews;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class BrasilSamsclubCrawler extends VTEXNewScraper {
   public BrasilSamsclubCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return session.getOptions().optString("homePage");
   }

   @Override
   protected Object fetch() {
      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setCookies(cookies)
         .setProxyservice(List.of(
            ProxyCollection.BUY,
            ProxyCollection.LUMINATI_SERVER_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_ROTATE_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_BR))
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new JsoupDataFetcher(), new FetcherDataFetcher(), new JavanetDataFetcher()), session, "get");
      return response;
   }

   @Override
   protected List<String> getMainSellersNames() {
      List<String> sellers = new ArrayList<>();
      JSONArray sellersJSON = session.getOptions().optJSONArray("sellers");

      for (int i = 0; i < sellersJSON.length(); i++) {
         sellers.add(sellersJSON.optString(i));
      }

      return sellers;
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }

   @Override
   protected String scrapDescription(Document doc, JSONObject productJson) {
      StringBuilder description = new StringBuilder();

      JSONArray descriptionArray = productJson.optJSONArray("Descrição");
      if (descriptionArray != null && !descriptionArray.isEmpty()) {
         for (int i = 0; i < descriptionArray.length(); i++) {
            description.append(descriptionArray.optString(i));
         }
      }

      return description.toString();
   }
}
