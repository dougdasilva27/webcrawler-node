package br.com.lett.crawlernode.crawlers.corecontent.portoalegre;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;

public class PortoalegreTreichelCrawler extends VTEXNewScraper {
   public PortoalegreTreichelCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return session.getOptions().optString("homePage");
   }

   @Override
   protected List<String> getMainSellersNames() {
      List<String> sellers = new ArrayList<>();
       session.getOptions().optJSONArray("sellers").forEach(seller -> sellers.add(seller.toString()));
      return sellers;
   }
   @Override
   protected String scrapPidFromApi(Document doc) {
      JSONObject jsonObject = CrawlerUtils.selectJsonFromHtml(doc, "script", "vtex.events.addData(", ");", false, true);
      return jsonObject.optString("productId");
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }
}
