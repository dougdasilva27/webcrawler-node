package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Collections;
import java.util.List;

public class RiodejaneiroSuperprixCrawler extends VTEXOldScraper {

   private static final String HOME_PAGE = "https://www.ipanema.superprix.com.br/";

   public RiodejaneiroSuperprixCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Collections.singletonList("superprix");
   }

   @Override
   protected boolean isMainRetailer(String sellerName) {
      return getMainSellersNames().stream().anyMatch(sellerName::contains);
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }

   @Override
   protected JSONObject crawlProductApi(String internalPid, String parameters) {
      return super.crawlProductApi(internalPid, null);
   }

}
