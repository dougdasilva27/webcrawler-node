package br.com.lett.crawlernode.crawlers.corecontent.guatemala;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Arrays;
import java.util.List;

public class GuatemalaPaizCrawler extends VTEXNewScraper {

   public GuatemalaPaizCrawler(Session session) {
      super(session);
   }

   @Override
   public void handleCookiesBeforeFetch() {

      Response response = dataFetcher.get(session, Request.RequestBuilder.create()
         .setUrl("https://www.paiz.com.gt/")
         .build());

      this.cookies = response.getCookies();
   }

   @Override
   protected String getHomePage() {
      return "https://www.paiz.com.gt/";
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Arrays.asList("paizgt");
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }
}
