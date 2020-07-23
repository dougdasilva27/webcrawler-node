package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.*;

import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXOldScraper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.RatingsReviews;
import models.Seller;
import models.Util;
import models.prices.Prices;

public class BrasilIkesakiCrawler extends VTEXOldScraper {

   private static final String HOME_PAGE = "http://www.ikesaki.com.br/";
   private static final List<String> MAIN_SELLER_NAME_LOWER = Collections.singletonList("ikesaki");

   public BrasilIkesakiCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return MAIN_SELLER_NAME_LOWER;
   }

   //When this crawler was made, no product with rating was found
   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return new RatingsReviews();
   }

   @Override
   protected String scrapDescription(Document doc, JSONObject productJson) {
      return CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".conteudo", ".detalhes"));
   }
}
