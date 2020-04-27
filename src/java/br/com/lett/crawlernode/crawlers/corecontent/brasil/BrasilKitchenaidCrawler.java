package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXNewScraper;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.RatingsReviews;

public class BrasilKitchenaidCrawler extends VTEXNewScraper {

   private static final String HOME_PAGE = "https://www.kitchenaid.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "Kitchenaid";

   public BrasilKitchenaidCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
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
   protected List<String> scrapSales(Document doc, JSONObject offerJson, String internalId, String internalPid) {
      String sale = CrawlerUtils.scrapStringSimpleInfo(doc, ".product__flags .price-flag", true);
      return sale != null && !sale.isEmpty() ? Arrays.asList(sale) : new ArrayList<>();
   }

   @Override
   protected Double scrapSpotlightPrice(Document doc, String internalId, JSONObject comertial) {
      Double spotlightPrice = super.scrapSpotlightPrice(doc, internalId, comertial);



      return spotlightPrice;
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }

}
