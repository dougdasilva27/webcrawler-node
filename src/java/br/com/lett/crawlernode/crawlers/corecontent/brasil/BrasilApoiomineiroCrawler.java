package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.*;

import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.session.Session;

public class BrasilApoiomineiroCrawler extends VTEXOldScraper {

   public BrasilApoiomineiroCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "http://www.apoioentrega.com/";
   private static final String HOME_PAGE_HTTPS = "https://www.apoioentrega.com/";
   private static final String MAIN_SELLER_NAME_LOWER = "apoio entrega";
   private static final String PACKAGE_API_URL = "https://www2.apoioentrega.com.br/api-vtex.php";

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE) || href.startsWith(HOME_PAGE_HTTPS));
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Arrays.asList(MAIN_SELLER_NAME_LOWER);
   }

   //The rating reviews api has changed and i cannot find any product with ratings to adapt the crawler
   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject apiJson) {
      return new RatingsReviews();
   }
}
