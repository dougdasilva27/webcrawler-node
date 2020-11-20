package br.com.lett.crawlernode.crawlers.corecontent.belohorizonte;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXCrawlersUtils;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VtexRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

/**
 * Date: 01/09/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BelohorizonteSupernossoCrawler extends VTEXOldScraper {

  private static final String HOME_PAGE = "https://www.supernossoemcasa.com.br/";
  private static final List<String> MAIN_SELLER_NAME_LOWER = Arrays.asList("super nosso em casa");

  public BelohorizonteSupernossoCrawler(Session session) {
    super(session);
    super.config.setMustSendRatingToKinesis(true);
  }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return MAIN_SELLER_NAME_LOWER;
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return new VtexRatingCrawler(session, HOME_PAGE, logger, cookies)
         .extractRatingAndReviews(internalId, doc, dataFetcher);
   }
}
