package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;

/**
 * Date: 2019-08-28
 *
 * @author gabriel
 */
public abstract class ArgentinaCarrefoursuper extends CarrefourCrawler {

   private static final String SELLER_FULL_NAME = "CARREFOUR";

   protected ArgentinaCarrefoursuper(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
   }

   @Override
   protected String getHomePage() {
      return "https://carrefour.com.ar/";
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Collections.singletonList(SELLER_FULL_NAME);
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }

}
