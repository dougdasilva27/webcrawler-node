package br.com.lett.crawlernode.crawlers.corecontent.costarica;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewImpl;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.RatingsReviews;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.List;
import java.util.stream.Collectors;

public class CostaricaMaspormenosCrawler extends VTEXNewImpl {


   public CostaricaMaspormenosCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
   }

   @Override
   protected String getHomePage() {
      return session.getOptions().optString("homePage");
   }

   @Override
   protected List<String> getMainSellersNames() {
      return session.getOptions().optJSONArray("sellers").toList().stream().map(Object::toString).collect(Collectors.toList());
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }

   @Override
   protected JSONObject crawlProductApi(String internalPid, String parameters) {
      JSONObject productApi = new JSONObject();

      String url = homePage + "api/catalog_system/pub/products/search?fq=productId:" + internalPid + "&sc=2";

      Request request = Request.RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      JSONArray array = CrawlerUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());

      if (!array.isEmpty()) {
         productApi = array.optJSONObject(0) == null ? new JSONObject() : array.optJSONObject(0);
      }

      return productApi;
   }
}
