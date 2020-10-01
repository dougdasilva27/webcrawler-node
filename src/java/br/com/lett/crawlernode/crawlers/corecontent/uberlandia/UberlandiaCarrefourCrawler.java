package br.com.lett.crawlernode.crawlers.corecontent.uberlandia;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.RequestsStatistics;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.brasil.BrasilCarrefourCrawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.CarrefourCrawler;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

/**
 * 18/04/2020
 * 
 * @author Fabrício
 *
 */
public class UberlandiaCarrefourCrawler extends CarrefourCrawler {

   public UberlandiaCarrefourCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocation() {
      return null;
   }

   @Override
   protected String getHomePage() {
      return null;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return null;
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }
}
