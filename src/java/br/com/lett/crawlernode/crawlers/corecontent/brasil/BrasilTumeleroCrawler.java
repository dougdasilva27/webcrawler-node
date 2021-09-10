package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXScraper;
import br.com.lett.crawlernode.util.CommonMethods;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Arrays;
import java.util.List;

public class BrasilTumeleroCrawler extends VTEXScraper {

   private static final String HOME_PAGE = "https://www.tumelero.com.br/";
   private static final String SELLER_NAME = "tumelero";

   public BrasilTumeleroCrawler(Session session) {
      super(session);
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
   protected String scrapInternalpid(Document doc) {
      String internalPid = "";
      String[] urlArray = session.getOriginalURL().split("=");

      if(urlArray.length > 0){
         internalPid = CommonMethods.getLast(urlArray);
      }

      return internalPid;
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, "73909", logger);
      return trustVox.extractRatingAndReviews(internalId, doc, dataFetcher);
   }
}
