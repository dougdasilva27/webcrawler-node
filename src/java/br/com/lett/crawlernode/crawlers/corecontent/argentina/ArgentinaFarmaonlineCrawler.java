package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXScraper;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Arrays;
import java.util.List;

/**
 * Date: 22/01/21
 *
 * @author Fellype Layunne
 */
public class ArgentinaFarmaonlineCrawler extends VTEXScraper {

   public static final String HOME_PAGE = "https://www.farmaonline.com/"; // this variable is also used in the ranking. Be careful when modifying
   public static final String SELLER_NAME = "FarmaOnline";

   public ArgentinaFarmaonlineCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
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
      return CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#___rc-p-id", "value");
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }
}
