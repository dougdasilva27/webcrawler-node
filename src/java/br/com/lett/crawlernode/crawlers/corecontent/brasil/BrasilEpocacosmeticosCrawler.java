package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class BrasilEpocacosmeticosCrawler extends VTEXOldScraper {

   private static final String HOME_PAGE = "https://www.epocacosmeticos.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "época cosméticos";

   public BrasilEpocacosmeticosCrawler(Session session) {
      super(session);
      config.setFetcher(FetchMode.FETCHER);
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
      TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, "393", logger);
      JSONObject json = crawlSkuJsonVTEX(doc, session);
      String id = json.optString("productId");
      if (id != null){
         return trustVox.extractRatingAndReviews(id, doc, dataFetcher);
      }
      return null;
   }


   @Override
   public JSONObject crawlSkuJsonVTEX(Document document, Session session) {
      Elements scriptTags = document.getElementsByTag("script");
      String scriptVariableName = "var skuJson_1 = ";
      JSONObject skuJson = new JSONObject();
      String skuJsonString = null;

      for (Element tag : scriptTags) {
         for (DataNode node : tag.dataNodes()) {
            if (tag.html().trim().startsWith(scriptVariableName)) {
               skuJsonString = node.getWholeData().split(Pattern.quote(scriptVariableName))[1]
                  + node.getWholeData().split(Pattern.quote(scriptVariableName))[1].split(Pattern.quote("};"))[0];
               break;
            }
         }
      }

      if (skuJsonString != null) {
         try {
            skuJson = new JSONObject(skuJsonString);

         } catch (JSONException e) {
            Logging.printLogWarn(logger, session, "Error creating JSONObject from var skuJson_0");
            Logging.printLogWarn(logger, session, CommonMethods.getStackTraceString(e));
         }
      }

      return skuJson;
   }


}
