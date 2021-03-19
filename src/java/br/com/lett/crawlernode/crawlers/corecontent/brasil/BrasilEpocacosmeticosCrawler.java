package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.Arrays;
import java.util.List;

import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrustvoxRatingCrawler;
import models.RatingsReviews;

public class BrasilEpocacosmeticosCrawler extends VTEXOldScraper {

   private static final String HOME_PAGE = "https://www.epocacosmeticos.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "época cosméticos";

   public BrasilEpocacosmeticosCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
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
      return Arrays.asList(MAIN_SELLER_NAME_LOWER);
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject apiJson) {
      TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, "284", logger);
      return trustVox.extractRatingAndReviewsForVtex(doc, dataFetcher).getRatingReviews(internalId);
   }

}
