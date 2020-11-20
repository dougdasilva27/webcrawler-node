
package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.Arrays;
import java.util.List;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import models.RatingsReviews;

public class BrasilSpicyCrawler extends VTEXOldScraper {

   private static final String HOME_PAGE = "https://www.spicy.com.br/";
   private static final List<String> SELLERS = Arrays.asList("SPICY");
   private static final String TRUST_VOX_STORE_ID = "74875";

   public BrasilSpicyCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return SELLERS;
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject apiJson) {
      TrustvoxRatingCrawler rating = new TrustvoxRatingCrawler(session, TRUST_VOX_STORE_ID, logger);
      return rating.extractRatingAndReviews(internalPid, doc, dataFetcher);
   }
}
